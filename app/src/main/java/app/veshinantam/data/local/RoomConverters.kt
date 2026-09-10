package app.veshinantam.data.local

import androidx.room.TypeConverter
import app.veshinantam.domain.model.MaterialType
import app.veshinantam.domain.model.MissedWorkBehavior
import app.veshinantam.domain.model.ScheduleKind
import app.veshinantam.domain.model.ScheduleState
import app.veshinantam.domain.model.TaskType
import java.time.Instant
import java.time.LocalDate

class RoomConverters {
    @TypeConverter fun localDateToString(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun instantToString(value: Instant?): String? = value?.toString()
    @TypeConverter fun stringToInstant(value: String?): Instant? = value?.let(Instant::parse)
    @TypeConverter fun scheduleKindToString(value: ScheduleKind) = value.name
    @TypeConverter fun stringToScheduleKind(value: String) = ScheduleKind.valueOf(value)
    @TypeConverter fun materialTypeToString(value: MaterialType) = value.name
    @TypeConverter fun stringToMaterialType(value: String) = MaterialType.valueOf(value)
    @TypeConverter fun missedBehaviorToString(value: MissedWorkBehavior) = value.name
    @TypeConverter fun stringToMissedBehavior(value: String) = MissedWorkBehavior.valueOf(value)
    @TypeConverter fun scheduleStateToString(value: ScheduleState) = value.name
    @TypeConverter fun stringToScheduleState(value: String) = ScheduleState.valueOf(value)
    @TypeConverter fun taskTypeToString(value: TaskType) = value.name
    @TypeConverter fun stringToTaskType(value: String) = TaskType.valueOf(value)
}

