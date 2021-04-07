package com.hw.orgpad;

import java.util.*;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Timetable extends AppCompatActivity {
	
    int curTasksNum;//Действие

    @Override
    protected void onCreate(Bundle savedInstanceState) {//Действие
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);
    }

    protected void onStart() {//Действие
		
        super.onStart();
        ListView listTasks = (ListView) findViewById(R.id.task_list);
		List<String> table = new ArrayList<String>();

                if(getSum(getCurrentTime(new GregorianCalendar()) == 0) //Если нет задач
                    table.add("Похоже что на сегодня задач нет.");
                else {
                    curTasksNum = 0;
                    TimeInterval interval;
                    Time startTime, endTime = new Time();
					boolean daynight = OrgPadDatabaseHelper.settings.daynight;

                    if (isWorkDay()) { //Рабочий или выходной день?
                        startTime = getStartTime(daynight); //Получение текущего времени либо близлежайшего свободного времени
                        startTime.roundUp(); //Округление минут до количества, делящегося на пять
                        endTime.setTime(OrgPadDatabaseHelper.settings.start);//Получение начала рабочих часов
                        interval = new TimeInterval(startTime, endTime);
                        table = putTasksInInterval(interval); //Заполнение задачами интервала до начала рабочих часов (Если он существует)

                        startTime.setTime(OrgPadDatabaseHelper.settings.end); //Получение конца рабочих часов
                        startTime.roundUp();
                        endTime = getFinishTime(); //Получение времени конца дня
                        interval = new TimeInterval(startTime, endTime);
                        table = putTasksInInterval(interval); //Заполнение задачами интервала от конца рабочих часов до конца дня (Если он существует)
                    } else {
                        startTime = getStartTime(daynight);
                        startTime.roundUp();
                        endTime = getFinishTime(daynight);
                        interval = new TimeInterval(startTime, endTime);
                        table = putTasksInInterval(interval); //Заполнение задачами интервала от начала до конца дня
                    }
                }
            ListView listGoals = (ListView) findViewById(R.id.list);
            ListAdapter listAdapter = new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_list_item_1,
                    table);
            listGoals.setAdapter(listAdapter);//Вывод данных на экран
    }

    private List<String> putTasksInInterval(TimeInterval interval, List<String> table, int cur)//Действие
    {
        System.out.println(interval.start + " - " + interval.end);
		int diff = OrgPadDatabaseHelper.settings.diff;
        int sum;
		
        for (Task choise; curTasksNum < getTasksNum(isWorkDay(), diff) && interval.start.less(interval.end); curTasksNum += getTaskValue(choise)) {
            sum = getSum(interval.start); //Получение суммы коэффициентов всех задач для данного момента времени
            choise = getChoise(sum, interval.start); //Получение случайной задачи (Чем выше коэффициент, тем выше вероятность получения)

            int time = getRandMinutes(choise.duration); //Получение случайной продолжительности задачи в зависимости от параметра "Продолжительность"
            interval.insertTaskFront(choise, time); //Получение случайной продолжительности задачи в зависимости от параметра "Продолжительность"
            if(interval.start.less(interval.end))
                table.add(interval.prevstart + " - " + interval.start + " : " + choise);
            else
                curTasksNum -= getTaskValue(choise);
        }
		return table;
    }

    private boolean isWorkDay()//Действие
    {
        GregorianCalendar newCal = new GregorianCalendar();
        int day = newCal.get( Calendar.DAY_OF_WEEK) - 1;
        day--;
        if(day == -1)
            day = 6;
        return OrgPadDatabaseHelper.settings.week[day];
    }
	
	private Time getFinishHours(boolean daynight)//Вычисление
    {
        int hours;
        if(daynight)
            hours = 24;
        else
            hours = 22;
        return hours;
    }

    private Time getFinishTime(boolean daynight)//Действие
    {
        Time time = new Time(getFinishHours(daynight), 0, 0);
        return time;
    }
	
	private Time getStartHours(boolean daynight)//Вычисление
    {
        int hours;
        if(daynight)
            hours = 10;
        else
            hours = 6;
        return hours;
    }
	
	private Time getMaxTime(Time curTime, Time time)//Вычисление
    {
        if(curTime.less(time))
            return time;
        else
            return curTime;
    }

    private Time getStartTime(boolean daynight)//Действие
    {
        Time time = new Time(getStartHours(daynight), 0, 0);
        Time curTime = getCurrentTime(new GregorianCalendar();

        return getMaxTime(Time curTime, Time time);
    }

    private Time getCurrentTime(GregorianCalendar gcalendar)//Действие
    {
        Time time = new Time(gcalendar.get(Calendar.HOUR_OF_DAY), gcalendar.get(Calendar.MINUTE), gcalendar.get(Calendar.SECOND));
        return time;
    }
	
	private int getMinutes(int duration, int random)//Вычисление
    {
        int rand;
        switch(duration) {
            case 2:
                rand = 30 + 5*random;
                break;
            case 3:
                rand = 60 + 5*random;
                break;
            default:
                rand = 15 + 5*random;
                break;
        }
        return rand;
    }

    private int getRandMinutes(int duration)//Действие
    {
		Random random = new Random();
        int rand = random.nextInt(2 * duration);
        return getMinutes(int duration, int rand);
    }

    private int getSum(Time time)//Действие
    {
        int sum = 0;
        for(int i = 0; i < OrgPadDatabaseHelper.goals.size(); i++)
            if(OrgPadDatabaseHelper.goals.get(i).isActive())
            for(int j = 0; j < OrgPadDatabaseHelper.goals.get(i).tasks.size(); j++)
            {
                if(OrgPadDatabaseHelper.goals.get(i).tasks.get(j).isActive())
                    sum += getPriorityCoeff(OrgPadDatabaseHelper.goals.get(i) ,OrgPadDatabaseHelper.goals.get(i).tasks.get(j), time);
            }

        return sum;
    }

    private int getTasksNum(boolean workDay, int diff)//Вычисление
    {
        if(workDay)
            return diff * 6;
        else
            return diff * 10;
    }
	
	private int getPriority(int result, int diff, int taskDiff, int prim, int tskNum)//Вычисление
    {
		if(((getTasksNum( isWorkDay(), diff) / 2) > curTasksNum && prim == 3 && taskDiff == 3)
                || ((getTasksNum( isWorkDay(), diff) / 2) > curTasksNum && prim == 1 && taskDiff == 1)
                || ((getTasksNum( isWorkDay(), diff) / 2) < curTasksNum && prim== 1 && taskDiff == 3)
                || ((getTasksNum( isWorkDay(), diff) / 2) < curTasksNum && prim == 3 && taskDiff == 1))
            result *= 5;
        if((diff == 3 && taskDiff == 3) || (diff == 1 && taskDiff == 1))
            result *= 2;
        return result;
	}
	
	private int getGoalValue(Task task, Goal goal)//Вычисление
    {
        return goal.priority * task.importance;
    }

    private int getPriorityCoeff(Goal goal, Task task, Time time)//Действие
    {
        int result = getGoalValue(task, goal);
		int diff = OrgPadDatabaseHelper.settings.diff;
		int taskDiff = task.difficulty;
		int prim = OrgPadDatabaseHelper.settings.primarily;
        return getPriority(int result, int diff, int taskDiff, int prim, curTasksNum);
    }

    private int getTaskValue(Task task)//Вычисление
    {
        return task.difficulty + task.duration;
    }
	
    private Task getChoise(int sum, Time time)//Действие
    {
		Random random = new Random();
        int rand = random.nextInt(sum);
        int s = 0;
        Task choise = null;
		boolean active;

        for(int i = 0; i < OrgPadDatabaseHelper.goals.size() && s <= rand; i++)
            if(OrgPadDatabaseHelper.goals.get(i).isActive())
            for(int j = 0; j < OrgPadDatabaseHelper.goals.get(i).tasks.size() && s <= rand; j++)
            {
                if(OrgPadDatabaseHelper.goals.get(i).tasks.get(j).isActive()) {
                    s += getPriorityCoeff(OrgPadDatabaseHelper.goals.get(i), OrgPadDatabaseHelper.goals.get(i).tasks.get(j), time);
                    choise = OrgPadDatabaseHelper.goals.get(i).tasks.get(j);
                }
            }

        return choise;
    }
}
