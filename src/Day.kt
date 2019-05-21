import java.time.DayOfWeek
import java.util.*
import kotlin.collections.ArrayList

class Day(name: DayOfWeek, private var partsOfHour: Int = 2) {

    private var hours = Array<ArrayList<Activity>>(HOURS_PER_DAY * partsOfHour) {ArrayList()} //size

    fun reduceMinActivityTime(newPartsOfHour: Int){
        require(newPartsOfHour % partsOfHour == 0)
        if(newPartsOfHour > partsOfHour) {
            val increment = newPartsOfHour / partsOfHour
            partsOfHour = newPartsOfHour
            val old = hours
            hours = Array(HOURS_PER_DAY * partsOfHour){i ->
                if(old[i/increment].isEmpty())
                    ArrayList<Activity>()
                else
                    old[i/increment]
            }
        }
    }

    //to be deleted
//    fun printHours(){
//        for(hour in hours)
//            println(hour.toString())
//        println()
//        println()
//    }

    private fun getBlockDuration(duration: Int): Int{
        return duration / 60 / partsOfHour
    }

    private fun getMinutesDuration(blocks: Int): Int{
        return blocks * 60 / partsOfHour
    }



    fun addActivity(activity: Activity, index: Int, duration: Int = -1){
        val recDur = if(duration != -1)
                duration
            else
                getBlockDuration(activity.duration)

        if(recDur != 0) {
            addActivity(activity, index + 1, recDur - 1) //
            //TODO("check for OB1 error")

            if (hours[index].isEmpty())
                hours[index].add(activity)
            else if (activity.parallelizable && hours[index][0].parallelizable) //need to check only first element for induction
                hours[index].add(activity)
            else
                throw ActivitiesNotCompatibleException("The hour you're trying to access is occupied and one of the activities is not serializable")
        }
    }

    fun addActivities(activities: List<Activity>, indexes: List<Int>){
        require(activities.size == indexes.size){ "every activity must have a index to be put to" }
        for(i in 0 until activities.size){
            addActivity(activities[i], indexes[i])
        }
    }

    fun isUrgent(deadline: Activity.Deadline, duration: Int, initCursor: Int = -1): Boolean{
        val iterator = TimeBlockIterator(this, initCursor, duration = getBlockDuration(duration))
        for(i in iterator){
            return deadline.getBlock(partsOfHour) > i.start + x
        }
        return true
    }

    fun getBlockIterator(deadline: Activity.Deadline? = null, duration: Int): TimeBlockIterator{
        return if(deadline != null)
            TimeBlockIterator(this, urgent = isUrgent(deadline, duration), duration = duration)
        else
            TimeBlockIterator(this, duration = duration)
    }


    //deprecated
    fun getFreeHours(): ArrayList<Int> {
        val freeHours = ArrayList<Int>()
        for(i in currentTimeRange() until hours.size){
            if(hours[i].isEmpty()){
                freeHours.add(i)
            }
        }
        return freeHours
    }

    //returns the current time range. ex: 17:20 -> 35 if partsOfHours == 2. should be the index of the array of hours
    private fun currentTimeRange(): Int{
        val currentTime = GregorianCalendar().get(GregorianCalendar.HOUR_OF_DAY) * partsOfHour
        return currentTime + ( GregorianCalendar().get(GregorianCalendar.MINUTE).toDouble() / 60 * partsOfHour ).toInt() + 1
    }

    fun putFirstFreeBlock(activity: Activity) {
        val iterator = TimeBlockIterator(this, getBlockDuration(activity.duration))
        addActivity(activity, iterator.next().start)
    }

    fun putWhilePostponing(activity: Activity, index: Int){

    }

    private fun postponable(activity: Activity, index: Int): Boolean{

        TODO()

    }

    data class TimeBlockIterate(val start: Int, val end: Int, val toBePostponed: ArrayList<Activity>)

    inner class TimeBlockIterator(private val day: Day, initCursor: Int = -1,
                                  val urgent: Boolean = false/*if urgent it returns also the blocks already scheduled but that can be postponed*/,
                                  private val duration: Int = 1): Iterator<TimeBlockIterate> {



        private var cursor: Int = if(initCursor != -1) initCursor
        else day.currentTimeRange()

        override fun hasNext(): Boolean {
            var x = duration
            for(i in cursor until day.hours.size) {
                if (day.hours[i].isEmpty()){
                    x--
                    if(x == 0)
                        return true
                }
                else
                    x = duration
            }
            return false
        }

        //returns the range of free block such as both ends are included [start, end] //countercomment: are you sure?
        override fun next(): TimeBlockIterate {
            for(i in cursor until day.hours.size){
                //TODO("set behavior when urgent = true")
                if(day.hours[i].isEmpty() || (day.hours[i].size < 2 && day.hours[i][0].parallelizable) || (urgent && !day.hours[i][0].parallelizable && postponable(day.hours[i][0], i))) {
                    var end = i
                    while (day.hours[end].isEmpty() ) {
                        end++
                    }
                    if(end - i >= duration) {
                        cursor = end
                        return TimeBlockIterate(i, end - 1, ArrayList())
                    }
                }
            }
            throw NoSuchElementException()
        }
    }


}
