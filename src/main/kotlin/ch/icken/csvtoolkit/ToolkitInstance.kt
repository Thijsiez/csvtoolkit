package ch.icken.csvtoolkit

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ch.icken.csvtoolkit.ToolkitInstance.InstanceSerializer
import ch.icken.csvtoolkit.file.CsvFile
import ch.icken.csvtoolkit.file.TabulatedFile
import ch.icken.csvtoolkit.transform.ConditionalTransformSet
import ch.icken.csvtoolkit.transform.FilterTransform
import ch.icken.csvtoolkit.transform.JoinTransform
import ch.icken.csvtoolkit.transform.MergeTransform
import ch.icken.csvtoolkit.transform.SelectTransform
import ch.icken.csvtoolkit.transform.SetTransform
import ch.icken.csvtoolkit.transform.SortTransform
import ch.icken.csvtoolkit.transform.Transform
import ch.icken.csvtoolkit.transform.Transform.ConditionParentTransform
import ch.icken.csvtoolkit.transform.Transform.ConditionalTransform
import ch.icken.csvtoolkit.transform.condition.AndCondition
import ch.icken.csvtoolkit.transform.condition.Condition
import ch.icken.csvtoolkit.transform.condition.Condition.ConditionParent
import ch.icken.csvtoolkit.transform.condition.NumericalCondition
import ch.icken.csvtoolkit.transform.condition.OrCondition
import ch.icken.csvtoolkit.transform.condition.RegexCondition
import ch.icken.csvtoolkit.transform.condition.TextCondition
import com.charleskorn.kaml.PolymorphismStyle.Property
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.io.Closeable
import java.io.File
import kotlin.coroutines.CoroutineContext

@Serializable(with = InstanceSerializer::class)
class ToolkitInstance() : CoroutineScope, Closeable {
    companion object {
        val serializationFormat = Yaml(
            serializersModule = SerializersModule {
                polymorphic(TabulatedFile::class) {
                    subclass(CsvFile::class)
                }
                polymorphic(Transform::class) {
                    polymorphic(ConditionParentTransform::class) {
                        subclass(ConditionalTransformSet::class)
                        subclass(FilterTransform::class)
                    }
                    polymorphic(ConditionalTransform::class) {
                        subclass(SetTransform::class)
                    }
                    subclass(ConditionalTransformSet::class)
                    subclass(FilterTransform::class)
                    subclass(JoinTransform::class)
                    subclass(MergeTransform::class)
                    subclass(SelectTransform::class)
                    subclass(SetTransform::class)
                    subclass(SortTransform::class)
                }
                polymorphic(Condition::class) {
                    polymorphic(ConditionParent::class) {
                        subclass(AndCondition::class)
                        subclass(OrCondition::class)
                    }
                    subclass(AndCondition::class)
                    subclass(NumericalCondition::class)
                    subclass(OrCondition::class)
                    subclass(RegexCondition::class)
                    subclass(TextCondition::class)
                }
            },
            configuration = YamlConfiguration(
                polymorphismStyle = Property
            )
        )
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            println("Whoops, we ran into a problem...")
            throwable.printStackTrace()
        }

    val files = mutableStateListOf<TabulatedFile>()
    val transforms = mutableStateListOf<Transform>()
    private var data: List<Map<String, String>> by mutableStateOf(emptyList())
    val observableData = derivedStateOf { data }

    var baseFileOverride: TabulatedFile? by mutableStateOf(null)
    val baseFile by derivedStateOf { baseFileOverride ?: files.firstOrNull() ?: throw NoSuchElementException() }

    val allowDoingTheThing by derivedStateOf {
        files.size >= 1 && files.filterIn(transforms.mapNotNull { it.usesFile }).all { it.isValid } &&
                transforms.size >= 1 && transforms.all { it.isValid(this) }
    }
    val allowDataExport by derivedStateOf { data.isNotEmpty() && data.first().isNotEmpty() }
    var isDoingTheThing by mutableStateOf(false); private set
    var currentlyProcessingTransform: Transform? by mutableStateOf(null); private set

    constructor(surrogate: InstanceSurrogate) : this() {
        files.addAll(surrogate.files.onEach { it.watchForChanges() })
        baseFileOverride = files.find { it.uuid == surrogate.baseFileUuid }
        transforms.addAll(surrogate.transforms.onEach { it.postDeserialization(this) })
    }

    fun headersUpTo(thisTransform: Transform, inclusive: Boolean = false): List<String> {
        return transforms
            .subList(0, transforms.indexOf(thisTransform) + if (inclusive) 1 else 0)
            .fold(baseFile.headers.toMutableList()) { intermediateHeaders, transform ->
                transform.doTheHeaderThing(intermediateHeaders)
            }
    }

    fun theThing() = launch {
        isDoingTheThing = true
        val finalData = baseFile.letData { data ->
            transforms.foldSuspendable(data.map { it.toMutableMap() }.toMutableList()) { intermediateData, transform ->
                currentlyProcessingTransform = transform
                transform.track { doTheActualThing(intermediateData) }
            }
        }
        files.forEach { it.unloadIfNecessary() }
        currentlyProcessingTransform = null
        isDoingTheThing = false
        launch(Dispatchers.Main) { data = finalData ?: emptyList() }
    }

    fun loadProject(file: File, onSuccess: (ToolkitInstance) -> Unit) = launch(Dispatchers.IO) {
        val instance = file.bufferedReader().use {
            serializationFormat.decodeFromString<ToolkitInstance>(it.readText())
        }
        launch(Dispatchers.Main) { onSuccess(instance) }
    }
    fun saveProject(file: File) = launch(Dispatchers.IO) {
        file.bufferedWriter().use {
            it.write(serializationFormat.encodeToString(this@ToolkitInstance))
        }
    }
    fun exportData(file: File) = launch(Dispatchers.IO) {
        if (data.isEmpty()) return@launch

        val headers = data.first().keys.toList()
        csvWriter().openAsync(file) {
            writeRow(headers)
            data.forEach { row ->
                writeRow(headers.map { row[it] ?: "" })
            }
        }
    }

    override fun close() {
        files.forEach { it.close() }
        cancel()
    }

    @Serializable
    class InstanceSurrogate(
        val files: List<TabulatedFile>,
        val baseFileUuid: String,
        val transforms: List<Transform>
    )
    object InstanceSerializer : KSerializer<ToolkitInstance> {
        override val descriptor = InstanceSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: ToolkitInstance) {
            encoder.encodeSerializableValue(InstanceSurrogate.serializer(),
                InstanceSurrogate(value.files, value.baseFile.uuid, value.transforms))
        }

        override fun deserialize(decoder: Decoder): ToolkitInstance {
            return ToolkitInstance(decoder.decodeSerializableValue(InstanceSurrogate.serializer()))
        }
    }
}