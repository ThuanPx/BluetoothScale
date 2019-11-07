/* Copyright (C) 2014  olie.xdev <olie.xdev@googlemail.com>
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>
*/

package com.hyperion.blescaleexample.core.datatypes;

import com.hyperion.blescaleexample.core.utils.Converters;
import com.hyperion.blescaleexample.core.utils.DateTimeHelpers;

import java.util.Calendar;
import java.util.Date;

public class ScaleUser {
    private int id;
    private String userName;
    private Date birthday;
    private float bodyHeight;
    private Converters.WeightUnit scaleUnit;
    private Converters.Gender gender;
    private float initialWeight;
    private float goalWeight;
    private Date goalDate;
    private Converters.MeasureUnit measureUnit;
    private Converters.ActivityLevel activityLevel;

    public ScaleUser() {
        userName = "";
        birthday = new Date();
        bodyHeight = -1;
        scaleUnit = Converters.WeightUnit.KG;
        gender = Converters.Gender.MALE;
        initialWeight = -1;
        goalWeight = -1;
        goalDate = new Date();
        measureUnit = Converters.MeasureUnit.CM;
        activityLevel = Converters.ActivityLevel.SEDENTARY;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public float getBodyHeight() {
        return bodyHeight;
    }

    public void setBodyHeight(float bodyHeight) {
        this.bodyHeight = bodyHeight;
    }

    public Converters.WeightUnit getScaleUnit() {
        return scaleUnit;
    }

    public void setScaleUnit(Converters.WeightUnit scaleUnit) {
        this.scaleUnit = scaleUnit;
    }

    public Converters.Gender getGender() {
        return gender;
    }

    public void setGender(Converters.Gender gender) {
        this.gender = gender;
    }

    public float getGoalWeight() {
        return goalWeight;
    }

    public void setGoalWeight(float goalWeight) {
        this.goalWeight = goalWeight;
    }

    public Date getGoalDate() {
        return goalDate;
    }

    public void setGoalDate(Date goalDate) {
        this.goalDate = goalDate;
    }

    public int getAge(Date todayDate) {
        Calendar calToday = Calendar.getInstance();
        if (todayDate != null) {
            calToday.setTime(todayDate);
        }

        Calendar calBirthday = Calendar.getInstance();
        calBirthday.setTime(birthday);

        return DateTimeHelpers.yearsBetween(calBirthday, calToday);
    }

    public int getAge() {
        return getAge(null);
    }

    public void setInitialWeight(float weight) {
        this.initialWeight = weight;
    }

    public float getInitialWeight() {
        return initialWeight;
    }

    public void setMeasureUnit(Converters.MeasureUnit unit) {
        measureUnit = unit;
    }

    public Converters.MeasureUnit getMeasureUnit() {
        return measureUnit;
    }

    public void setActivityLevel(Converters.ActivityLevel level) {
        activityLevel = level;
    }

    public Converters.ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public static String getPreferenceKey(int userId, String key) {
        return String.format("user.%d.%s", userId, key);
    }

    public String getPreferenceKey(String key) {
        return getPreferenceKey(getId(), key);
    }

    @Override
    public String toString()
    {
        return String.format(
                "id(%d) name(%s) birthday(%s) age(%d) body height(%.2f) scale unit(%s) " +
                "gender(%s) initial weight(%.2f) goal weight(%.2f) goal date(%s) " +
                "measure unt(%s) activity level(%d)",
                id, userName, birthday.toString(), getAge(), bodyHeight, scaleUnit.toString(),
                gender.toString().toLowerCase(), initialWeight, goalWeight, goalDate.toString(),
                measureUnit.toString(), activityLevel.toInt());
    }
}
