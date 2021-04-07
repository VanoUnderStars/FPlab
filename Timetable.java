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

    private List<Task> applicants = new ArrayList<Task>(); //Действие
    private List<String> table = new ArrayList<String>(); //Действие
    private Random random = new Random(); //Действие
    int curTasksNum; //Действие

    @Override
    protected void onCreate(Bundle savedInstanceState) { //Действие
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);
    }

    protected void onStart() { //Действие
        super.onStart();
        ListView listTasks = (ListView) findViewById(R.id.task_list);


                if(getSum(getCurrentTime()) == 0) //Если нет задач
                    table.add("Похоже что на сегодня задач нет.");
                else {
                    curTasksNum = 0;
                    TimeInterval interval;
                    Time startTime, endTime = new Time();

                    if (isWorkDay()) { //Рабочий или выходной день?
                        startTime = getStartTime(); //Получение текущего времени либо близлежайшего свободного времени
                        startTime.roundUp(); //Округление минут до количества, делящегося на пять
                        endTime.setTime(OrgPadDatabaseHelper.settings.start);//Получение начала рабочих часов
                        interval = new TimeInterval(startTime, endTime);
                        putTasksInInterval(interval); //Заполнение задачами интервала до начала рабочих часов (Если он существует)

                        startTime.setTime(OrgPadDatabaseHelper.settings.end); //Получение конца рабочих часов
                        startTime.roundUp();
                        endTime = getFinishTime(); //Получение времени конца дня
                        interval = new TimeInterval(startTime, endTime);
                        putTasksInInterval(interval); //Заполнение задачами интервала от конца рабочих часов до конца дня (Если он существует)
                    } else {
                        startTime = getStartTime();
                        startTime.roundUp();
                        endTime = getFinishTime();
                        interval = new TimeInterval(startTime, endTime);
                        putTasksInInterval(interval); //Заполнение задачами интервала от начала до конца дня
                    }
                }
            ListView listGoals = (ListView) findViewById(R.id.list);
            ListAdapter listAdapter = new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_list_item_1,
                    table);
            listGoals.setAdapter(listAdapter);//Вывод данных на экран
    }

    private void putTasksInInterval(TimeInterval interval) //Действие
    {
        System.out.println(interval.start + " - " + interval.end);
        int sum;
        for (Task choise; curTasksNum < getTasksNum() && interval.start.less(interval.end); curTasksNum += getTaskValue(choise)) {
            sum = getSum(interval.start); //Получение суммы коэффициентов всех задач для данного момента времени
            choise = getChoise(sum, interval.start); //Получение случайной задачи (Чем выше коэффициент, тем выше вероятность получения)

            int time = getRandMinutes(choise.duration); //Получение случайной продолжительности задачи в зависимости от параметра "Продолжительность"
            //System.out.println(time);
            interval.insertTaskFront(choise, time); //Получение случайной продолжительности задачи в зависимости от параметра "Продолжительность"
            if(interval.start.less(interval.end))
                table.add(interval.prevstart + " - " + interval.start + " : " + choise);
            else
                curTasksNum -= getTaskValue(choise);
        }
    }

    private boolean isWorkDay() //Действие
    {
        GregorianCalendar newCal = new GregorianCalendar();
        int day = newCal.get( Calendar.DAY_OF_WEEK ) - 1;
        day--;
        if(day == -1)
            day = 6;
        return OrgPadDatabaseHelper.settings.week[day];
    }

    private Time getFinishTime() //Действие
    {
        int hours;
        if(OrgPadDatabaseHelper.settings.daynight)
            hours = 24;
        else
            hours = 22;
        Time time = new Time(hours, 0, 0);
        return time;
    }

    private Time getStartTime() //Действие
    {
        int hours;
        if(OrgPadDatabaseHelper.settings.daynight)
            hours = 10;
        else
            hours = 6;
        Time time = new Time(hours, 0, 0);
        Time curTime = getCurrentTime();

        if(curTime.less(time))
            return time;
        else
            return curTime;
    }

    private Time getCurrentTime() //Действие
    {
        GregorianCalendar gcalendar = new GregorianCalendar();
        Time time = new Time(11/*gcalendar.get(Calendar.HOUR_OF_DAY)*/, gcalendar.get(Calendar.MINUTE), gcalendar.get(Calendar.SECOND));
        return time;
    }

    private int getRandMinutes(int duration) //Действие
    {
        int rand;
        switch(duration) {
            case 2:
                rand = 30 + 5*random.nextInt(6);
                break;
            case 3:
                rand = 60 + 5*random.nextInt(6);
                break;
            default:
                rand = 15 + 5*random.nextInt(3);
                break;
        }
        return rand;
    }

    private int getSum(Time time) //Действие
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

    private int getTasksNum() //Действие
    {
        if(isWorkDay())
            return OrgPadDatabaseHelper.settings.diff * 6;
        else
            return OrgPadDatabaseHelper.settings.diff * 10;
    }

    private int getPriorityCoeff(Goal goal, Task task, Time time) //Действие
    {
        int result = goal.priority * task.importance;
        if(((getTasksNum() / 2) > curTasksNum && OrgPadDatabaseHelper.settings.primarily == 3 && task.difficulty == 3)
                || ((getTasksNum() / 2) > curTasksNum && OrgPadDatabaseHelper.settings.primarily == 1 && task.difficulty == 1)
                || ((getTasksNum() / 2) < curTasksNum && OrgPadDatabaseHelper.settings.primarily == 1 && task.difficulty == 3)
                || ((getTasksNum() / 2) < curTasksNum && OrgPadDatabaseHelper.settings.primarily == 3 && task.difficulty == 1))
            result *= 5;
        if((OrgPadDatabaseHelper.settings.diff == 3 && task.difficulty == 3) || (OrgPadDatabaseHelper.settings.diff == 1 && task.difficulty == 1))
            result *= 2;
        return result;
    }

    private int getTaskValue(Task task) //Вычисление
    {
        return task.difficulty + task.duration;
    }

    private Task getChoise(int sum, Time time) //Действие
    {
        int rand = random.nextInt(sum);
        int s = 0;
        Task choise = null;


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
