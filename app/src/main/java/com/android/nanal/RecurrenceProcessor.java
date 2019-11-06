package com.android.nanal;
/* //device/content/providers/pim/RecurrenceProcessor.java
 **
 ** Copyright 2006, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

import android.text.format.Time;
import android.util.Log;

import com.android.nanal.event.EventRecurrence;

import java.util.TreeSet;

public class RecurrenceProcessor
{
    // these are created once and reused.
    // 한 번 생성되고 다시 사용됨
    private Time mIterator = new Time(Time.TIMEZONE_UTC);
    private Time mUntil = new Time(Time.TIMEZONE_UTC);
    private StringBuilder mStringBuilder = new StringBuilder();
    private Time mGenerated = new Time(Time.TIMEZONE_UTC);
    private DaySet mDays = new DaySet(false);
    // Give up after this many loops.  This is roughly 1 second of expansion.
    // 많은 루프 후 포기. 대략 1초의 팽창
    private static final int MAX_ALLOWED_ITERATIONS = 2000;

    public RecurrenceProcessor()
    {
    }

    private static final String TAG = "RecurrenceProcessor";

    private static final boolean SPEW = false;

    /**
     * Returns the time (millis since epoch) of the last occurrence,
     * or -1 if the event repeats forever.  If there are no occurrences
     * (because the exrule or exdates cancel all the occurrences) and the
     * event does not repeat forever, then 0 is returned.
     *
     * 마지막 발생의 시간 (epoch 이후)을 반환하고, 이벤트가 계속 반복될 경우 -1을 반환.
     * 발생이 없거나 ( exrule이나 exdate가 모든 발생을 취소하기 때문에) 이벤트가 영구적으로 반복되지 않으면 0이 반환
     *
     * This computes a conservative estimate of the last occurrence. That is,
     * the time of the actual last occurrence might be earlier than the time
     * returned by this method.
     *
     * 마지막 발생에 대한 최소 추정치를 계산
     * 즉 실제 마지막 발생 시간은 이 방법에 의해 반환되는 시간보다 더 빠를 수 있음
     *
     * @param dtstart the time of the first occurrence
     * @param recur the recurrence
     * @return an estimate of the time (in UTC milliseconds) of the last
     * occurrence, which may be greater than the actual last occurrence
     * @throws DateException
     *
     * dtstart -> 최초 발생 시간
     * recur -> 반복
     * return -> 마지막 발생 시간(UTC 밀리초 단위)의 추정치. 실제 마지막 발생 시간보다 클 수 있음
     * 예외 : DeteException
     */

    public long getLastOccurence(Time dtstart,
                                 RecurrenceSet recur) throws DateException {
        return getLastOccurence(dtstart, null /* no limit */, recur);
    }

    /**
     * Returns the time (millis since epoch) of the last occurrence,
     * or -1 if the event repeats forever.  If there are no occurrences
     * (because the exrule or exdates cancel all the occurrences) and the
     * event does not repeat forever, then 0 is returned.
     *
     * 마지막 발생의 시간 (epoch 이후)을 반환하고, 이벤트가 계속 반복될 경우 -1을 반환.
     * 발생이 없거나 ( exrule이나 exdate가 모든 발생을 취소하기 때문에) 이벤트가 영구적으로 반복되지 않으면 0이 반환
     *
     * This computes a conservative estimate of the last occurrence. That is,
     * the time of the actual last occurrence might be earlier than the time
     * returned by this method.
     *
     * 마지막 발생에 대한 최소 추정치를 계산
     * 즉 실제 마지막 발생 시간은 이 방법에 의해 반환되는 시간보다 더 빠를 수 있음
     *
     * @param dtstart the time of the first occurrence
     * @param maxtime the max possible time of the last occurrence. null means no limit
     * @param recur the recurrence
     * @return an estimate of the time (in UTC milliseconds) of the last
     * occurrence, which may be greater than the actual last occurrence
     * @throws DateException
     *
     * dtstart -> 최초 발생 시간
     * recur -> 반복
     * return -> 마지막 발생 시간(UTC 밀리초 단위)의 추정치. 실제 마지막 발생 시간보다 클 수 있음
     * 예외 : DeteException
     */

    public long getLastOccurence(Time dtstart, Time maxtime,
                                 RecurrenceSet recur) throws DateException {
        long lastTime = -1;
        boolean hasCount = false;

        // first see if there are any "until"s specified.  if so, use the latest
        // until / rdate.
        // 지정된 "until"이 있는지 확인한 후 만약 그렇다면 가장 최근의 until/rdate를 사용
        if (recur.rrules != null) {
            for (EventRecurrence rrule : recur.rrules) {
                if (rrule.count != 0) {
                    hasCount = true;
                } else if (rrule.until != null) {
                    // according to RFC 2445, until must be in UTC.
                    mIterator.parse(rrule.until);
                    long untilTime = mIterator.toMillis(false /* use isDst */);
                    if (untilTime > lastTime) {
                        lastTime = untilTime;
                    }
                }
            }
            if (lastTime != -1 && recur.rdates != null) {
                for (long dt : recur.rdates) {
                    if (dt > lastTime) {
                        lastTime = dt;
                    }
                }
            }

            // If there were only "until"s and no "count"s, then return the
            // last "until" date or "rdate".
            // "until"만 있고 "count"가 없으면 마지막 "until" 날짜 또는 "rdate"를 반환
            if (lastTime != -1 && !hasCount) {
                return lastTime;
            }
        } else if (recur.rdates != null &&
                recur.exrules == null && recur.exdates == null) {
            // if there are only rdates, we can just pick the last one.
            // 만약 rdate만 있다면 마지막 하나를 선택할 수 있음
            for (long dt : recur.rdates) {
                if (dt > lastTime) {
                    lastTime = dt;
                }
            }
            return lastTime;
        }

        // Expand the complete recurrence if there were any counts specified,
        // or if there were rdates specified.
        // 지정된 count가 있거나 rdate가 지정된 경우 전체 반복 확장
        if (hasCount || recur.rdates != null || maxtime != null) {
            // The expansion might not contain any dates if the exrule or
            // exdates cancel all the generated dates.
            // exrule 또는 exdate가 생성된 날짜를 모두 취소하는 경우 확장에는 날짜가 포함되지 않을 수 있음
            long[] dates = expand(dtstart, recur,
                    dtstart.toMillis(false /* use isDst */) /* range start */,
                    (maxtime != null) ?
                            maxtime.toMillis(false /* use isDst */) : -1 /* range end */);

            // The expansion might not contain any dates if exrule or exdates
            // cancel all the generated dates.
            // exrule이나 exdate가 발생된 날짜를 모두 취소할 경우 확장에 날짜가 포함되지 않을 수 있음
            if (dates.length == 0) {
                return 0;
            }
            return dates[dates.length - 1];
        }
        return -1;
    }

    /**
     * a -- list of values 값의 리스트
     * N -- number of values to use in a 사용할 값의 수
     * v -- value to check for 확인할 값
     */
    private static boolean listContains(int[] a, int N, int v)
    {
        for (int i=0; i<N; i++) {
            if (a[i] == v) {
                return true;
            }
        }
        return false;
    }

    /**
     * a -- list of values 값의 리스트
     * N -- number of values to use in a 사용할 값의 수
     * v -- value to check for 확인할 값
     * max -- if a value in a is negative, add that negative value
     *        to max and compare that instead; this is how we deal with
     *        negative numbers being offsets from the end value
     *        a의 값이 음수인 경우, 음의 값을 최대값에 추가하고 비교
     *        이렇게 할 경우 최종값에서 음의 수가 상쇄되는 것을 처리할 수 있음
     */
    private static boolean listContains(int[] a, int N, int v, int max)
    {
        for (int i=0; i<N; i++) {
            int w = a[i];
            if (w > 0) {
                if (w == v) {
                    return true;
                }
            } else {
                max += w; // w is negative
                if (max == v) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Filter out the ones for events whose BYxxx rule is for
     * a period greater than or equal to the period of the FREQ.
     *
     * Returns 0 if the event should not be filtered out
     * Returns something else (a rule number which is useful for debugging)
     * if the event should not be returned
     *
     * BYxxx 규칙이 FREQ 기간보다 크거나 같은 기간인 이벤트에 대해 해당 규칙 필터링
     * 이벤트를 필터링하지 않을 경우 0을 반환
     * 이벤트가 반환되지 않았을 경우 다른 항목(디버깅에 유용한 규칙 번호) 반환
     */
    private static int filter(EventRecurrence r, Time iterator)
    {
        boolean found;
        int freq = r.freq;

        if (EventRecurrence.MONTHLY >= freq) {
            // BYMONTH
            if (r.bymonthCount > 0) {
                found = listContains(r.bymonth, r.bymonthCount,
                        iterator.month + 1);
                if (!found) {
                    return 1;
                }
            }
        }
        if (EventRecurrence.WEEKLY >= freq) {
            // BYWEEK -- this is just a guess.  I wonder how many events
            // acutally use BYWEEKNO.
            if (r.byweeknoCount > 0) {
                found = listContains(r.byweekno, r.byweeknoCount,
                        iterator.getWeekNumber(),
                        iterator.getActualMaximum(Time.WEEK_NUM));
                if (!found) {
                    return 2;
                }
            }
        }
        if (EventRecurrence.DAILY >= freq) {
            // BYYEARDAY
            if (r.byyeardayCount > 0) {
                found = listContains(r.byyearday, r.byyeardayCount,
                        iterator.yearDay, iterator.getActualMaximum(Time.YEAR_DAY));
                if (!found) {
                    return 3;
                }
            }
            // BYMONTHDAY
            if (r.bymonthdayCount > 0 ) {
                found = listContains(r.bymonthday, r.bymonthdayCount,
                        iterator.monthDay,
                        iterator.getActualMaximum(Time.MONTH_DAY));
                if (!found) {
                    return 4;
                }
            }
            // BYDAY -- when filtering, we ignore the number field, because it
            // only is meaningful when creating more events.
            // 필터링할 때 숫자 필드는 더 많은 이벤트를 생성할 때만 의미가 있기 때문에 무시
            byday:
            if (r.bydayCount > 0) {
                int a[] = r.byday;
                int N = r.bydayCount;
                int v = EventRecurrence.timeDay2Day(iterator.weekDay);
                for (int i=0; i<N; i++) {
                    if (a[i] == v) {
                        break byday;
                    }
                }
                return 5;
            }
        }
        if (EventRecurrence.HOURLY >= freq) {
            // BYHOUR
            found = listContains(r.byhour, r.byhourCount,
                    iterator.hour,
                    iterator.getActualMaximum(Time.HOUR));
            if (!found) {
                return 6;
            }
        }
        if (EventRecurrence.MINUTELY >= freq) {
            // BYMINUTE
            found = listContains(r.byminute, r.byminuteCount,
                    iterator.minute,
                    iterator.getActualMaximum(Time.MINUTE));
            if (!found) {
                return 7;
            }
        }
        if (EventRecurrence.SECONDLY >= freq) {
            // BYSECOND
            found = listContains(r.bysecond, r.bysecondCount,
                    iterator.second,
                    iterator.getActualMaximum(Time.SECOND));
            if (!found) {
                return 8;
            }
        }

        if (r.bysetposCount > 0) {
            bysetpos:
            // BYSETPOS - we only handle rules like FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1
            if (freq == EventRecurrence.MONTHLY && r.bydayCount > 0) {
                // Check for stuff like BYDAY=1TU
                for (int i = r.bydayCount - 1; i >= 0; i--) {
                    if (r.bydayNum[i] != 0) {
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "BYSETPOS not supported with these rules: " + r);
                        }
                        break bysetpos;
                    }
                }
                if (!filterMonthlySetPos(r, iterator)) {
                    // Not allowed, filter it out.
                    return 9;
                }
            } else {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "BYSETPOS not supported with these rules: " + r);
                }
            }
            // BYSETPOS was defined but we don't know how to handle it.  Do no filtering based
            // on it.
        }

        // if we got to here, we didn't filter it out
        return 0;
    }

    /**
     * Filters out instances that don't match the BYSETPOS clause of a monthly recurrence rule.
     * This is an awkward and inefficient way to go about it.
     *
     * @returns true if this instance should be kept
     *
     * 월별 반복 규칙의 BYSETPOS 절과 일치하지 않는 인스턴스 필터.
     * 이것은 그것을 처리하는데에 어색하고 비효율적인 방법
     */
    private static boolean filterMonthlySetPos(EventRecurrence r, Time instance) {
        /*
         * Compute the day of the week for the first day of the month.  "instance" has a
         * day number and a DotW, so we compute the DotW of the 1st from that.  Note DotW
         * is 0-6, where 0=SUNDAY.
         *
         * 그 달의 첫날에 대한 요일을 계산.
         * "인스턴스"는 날짜번호와 DotW가 있으므로 그것으로 1일의 DotW를 계산
         * DotW의 범위는 0~6이며 여기서 0은 SUNDAY
         *
         * The basic calculation is to take the instance's "day of the week" number, subtract
         * (day of the month - 1) mod 7, and then make sure it's positive.  We can simplify
         * that with some algebra.
         *
         * 기본 계산은 인스턴스의 "요일"번호를 빼고 (월 - 1일) mod 7을 뺀 후 양수인지 확인
         */
        int dotw = (instance.weekDay - instance.monthDay + 36) % 7;

        /*
         * The byday[] values are specified as bits, so we can just OR them all
         * together.
         *
         * 배열 byday 값은 비트로 지정
         */
        int bydayMask = 0;
        for (int i = 0; i < r.bydayCount; i++) {
            bydayMask |= r.byday[i];
        }

        /*
         * Generate a set according to the BYDAY rules.  For each day of the month, determine
         * if its day of the week is included.  If so, append it to the day set.
         *
         * BYDAY 규칙에 따라 set 생성.
         * 해당 월의 각 요일에 대해 해당 요일을 포함하는지 여부 결정
         * 만약 그렇다면 dat set에 추가
         */
        int maxDay = instance.getActualMaximum(Time.MONTH_DAY);
        int daySet[] = new int[maxDay];
        int daySetLength = 0;

        for (int md = 1; md <= maxDay; md++) {
            // For each month day, see if it's part of the set.  (This makes some assumptions
            // about the exact form of the DotW constants.)
            // 매 달마다 해당 달이 set의 일부인지 확인
            int dayBit = EventRecurrence.SU << dotw;
            if ((bydayMask & dayBit) != 0) {
                daySet[daySetLength++] = md;
            }

            dotw++;
            if (dotw == 7)
                dotw = 0;
        }

        /*
         * Now walk through the BYSETPOS list and see if the instance is equal to any of the
         * specified daySet entries.
         *
         * BYDETPOS 목록에서 인스턴스가 지정된 deySet 항목 중 하나와 동일한지 확인
         */
        for (int i = r.bysetposCount - 1; i >= 0; i--) {
            int index = r.bysetpos[i];
            if (index > 0) {
                if (index > daySetLength) {
                    continue;  // out of range
                }
                if (daySet[index-1] == instance.monthDay) {
                    return true;
                }
            } else if (index < 0) {
                if (daySetLength + index < 0) {
                    continue;  // out of range
                }
                if (daySet[daySetLength + index] == instance.monthDay) {
                    return true;
                }
            } else {
                // should have been caught by parser
                throw new RuntimeException("invalid bysetpos value");
            }
        }

        return false;
    }


    private static final int USE_ITERATOR = 0;
    private static final int USE_BYLIST = 1;

    /**
     * Return whether we should make this list from the BYxxx list or
     * from the component of the iterator.
     *
     * 해당 목록을 BYxxx 리스트 또는 iterator의 컴포넌트에서 만들어야 하는지 여부 반환
     */
    int generateByList(int count, int freq, int byFreq)
    {
        if (byFreq >= freq) {
            return USE_ITERATOR;
        } else {
            if (count == 0) {
                return USE_ITERATOR;
            } else {
                return USE_BYLIST;
            }
        }
    }

    private static boolean useBYX(int freq, int freqConstant, int count)
    {
        return freq > freqConstant && count > 0;
    }

    public static class DaySet
    {
        public DaySet(boolean zulu)
        {
            mTime = new Time(Time.TIMEZONE_UTC);
        }

        void setRecurrence(EventRecurrence r)
        {
            mYear = 0;
            mMonth = -1;
            mR = r;
        }

        boolean get(Time iterator, int day)
        {
            int realYear = iterator.year;
            int realMonth = iterator.month;

            Time t = null;

            if (SPEW) {
                Log.i(TAG, "get called with iterator=" + iterator
                        + " " + iterator.month
                        + "/" + iterator.monthDay
                        + "/" + iterator.year + " day=" + day);
            }
            if (day < 1 || day > 28) {
                // if might be past the end of the month, we need to normalize it
                t = mTime;
                t.set(day, realMonth, realYear);
                unsafeNormalize(t);
                realYear = t.year;
                realMonth = t.month;
                day = t.monthDay;
                if (SPEW) {
                    Log.i(TAG, "normalized t=" + t + " " + t.month
                            + "/" + t.monthDay
                            + "/" + t.year);
                }
            }

            /*
            if (true || SPEW) {
                Log.i(TAG, "set t=" + t + " " + realMonth + "/" + day + "/" + realYear);
            }
            */
            if (realYear != mYear || realMonth != mMonth) {
                if (t == null) {
                    t = mTime;
                    t.set(day, realMonth, realYear);
                    unsafeNormalize(t);
                    if (SPEW) {
                        Log.i(TAG, "set t=" + t + " " + t.month
                                + "/" + t.monthDay
                                + "/" + t.year
                                + " realMonth=" + realMonth + " mMonth=" + mMonth);
                    }
                }
                mYear = realYear;
                mMonth = realMonth;
                mDays = generateDaysList(t, mR);
                if (SPEW) {
                    Log.i(TAG, "generated days list");
                }
            }
            return (mDays & (1<<day)) != 0;
        }

        /**
         * Fill in a bit set containing the days of the month on which this
         * will occur.
         *
         * 해당 월의 요일이 포함된 bit set을 채움
         *
         * Only call this if the r.freq > DAILY.  Otherwise, we should be
         * processing the BYDAY, BYMONTHDAY, etc. as filters instead.
         *
         * r.frq > DAILY 인 경우 호출
         * 그렇지 않은 경우 BYDAY, BYMONTHDAY 등을 필터로 처리
         *
         * monthOffset may be -1, 0 or 1
         */
        private static int generateDaysList(Time generated, EventRecurrence r)
        {
            int days = 0;

            int i, count, v;
            int[] byday, bydayNum, bymonthday;
            int j, lastDayThisMonth;
            int first; // Time.SUNDAY, etc
            int k;

            lastDayThisMonth = generated.getActualMaximum(Time.MONTH_DAY);

            // BYDAY
            count = r.bydayCount;
            if (count > 0) {
                // calculate the day of week for the first of this month (first)
                // 이달의 첫째 요일 계산
                j = generated.monthDay;
                while (j >= 8) {
                    j -= 7;
                }
                first = generated.weekDay;
                if (first >= j) {
                    first = first - j + 1;
                } else {
                    first = first - j + 8;
                }

                // What to do if the event is weekly:
                // This isn't ideal, but we'll generate a month's worth of events
                // and the code that calls this will only use the ones that matter
                // for the current week.
                // 이벤트가 매주 있는 경우 수행할 작업
                byday = r.byday;
                bydayNum = r.bydayNum;
                for (i=0; i<count; i++) {
                    v = bydayNum[i];
                    j = EventRecurrence.day2TimeDay(byday[i]) - first + 1;
                    if (j <= 0) {
                        j += 7;
                    }
                    if (v == 0) {
                        // v is 0, each day in the month/week
                        for (; j<=lastDayThisMonth; j+=7) {
                            if (SPEW) Log.i(TAG, "setting " + j + " for rule "
                                    + v + "/" + EventRecurrence.day2TimeDay(byday[i]));
                            days |= 1 << j;
                        }
                    }
                    else if (v > 0) {
                        // v is positive, count from the beginning of the month
                        // -1 b/c the first one should add 0
                        j += 7*(v-1);
                        if (j <= lastDayThisMonth) {
                            if (SPEW) Log.i(TAG, "setting " + j + " for rule "
                                    + v + "/" + EventRecurrence.day2TimeDay(byday[i]));
                            // if it's impossible, we drop it
                            days |= 1 << j;
                        }
                    }
                    else {
                        // v is negative, count from the end of the month
                        // find the last one
                        for (; j<=lastDayThisMonth; j+=7) {
                        }
                        // v is negative
                        // should do +1 b/c the last one should add 0, but we also
                        // skipped the j -= 7 b/c the loop to find the last one
                        // overshot by one week
                        j += 7*v;
                        if (j >= 1) {
                            if (SPEW) Log.i(TAG, "setting " + j + " for rule "
                                    + v + "/" + EventRecurrence.day2TimeDay(byday[i]));
                            days |= 1 << j;
                        }
                    }
                }
            }

            // BYMONTHDAY
            // Q: What happens if we have BYMONTHDAY and BYDAY?
            // A: I didn't see it in the spec, so in lieu of that, we'll
            // intersect the two.  That seems reasonable to me.
            if (r.freq > EventRecurrence.WEEKLY) {
                count = r.bymonthdayCount;
                if (count != 0) {
                    bymonthday = r.bymonthday;
                    if (r.bydayCount == 0) {
                        for (i=0; i<count; i++) {
                            v = bymonthday[i];
                            if (v >= 0) {
                                days |= 1 << v;
                            } else {
                                j = lastDayThisMonth + v + 1; // v is negative
                                if (j >= 1 && j <= lastDayThisMonth) {
                                    days |= 1 << j;
                                }
                            }
                        }
                    } else {
                        // This is O(lastDayThisMonth*count), which is really
                        // O(count) with a decent sized constant.
                        for (j=1; j<=lastDayThisMonth; j++) {
                            next_day : {
                                if ((days&(1<<j)) != 0) {
                                    for (i=0; i<count; i++) {
                                        if (bymonthday[i] == j) {
                                            break next_day;
                                        }
                                    }
                                    days &= ~(1<<j);
                                }
                            }
                        }
                    }
                }
            }
            return days;
        }

        private EventRecurrence mR;
        private int mDays;
        private Time mTime;
        private int mYear;
        private int mMonth;
    }

    /**
     * Expands the recurrence within the given range using the given dtstart
     * value. Returns an array of longs where each element is a date in UTC
     * milliseconds. The return value is never null.  If there are no dates
     * then an array of length zero is returned.
     *
     * 주어진 dtstart 값을 사용하여 주어진 범위 내에서 일정 반복을 확장
     * 각 요소가 UTC 밀리초 단위의 날짜인 긴 배열을 반환
     * 반환 값은 NULL이 될 수 없음
     * 날짜가 없으면 길이 0의 배열 반환
     *
     * @param dtstart a Time object representing the first occurrence
     * @param recur the recurrence rules, including RRULE, RDATES, EXRULE, and
     * EXDATES
     * @param rangeStartMillis the beginning of the range to expand, in UTC
     * milliseconds
     * @param rangeEndMillis the non-inclusive end of the range to expand, in
     * UTC milliseconds; use -1 for the entire range.
     * @return an array of dates, each date is in UTC milliseconds
     * @throws DateException
     * @throws android.util.TimeFormatException if recur cannot be parsed
     *
     * dtstart -> 첫 발생을 나타내는 시간 객체
     * recur -> 반복 규칙. RRULE, RDATES, EXRULE, EXDATES 포함
     * rangeStartMillis -> 확장할 범위의 시작 (UTC 밀리초)
     * rangeEndMillis -> 확장할 비포함 범위 끝 (UTC 밀리초). 전체 범위에서 -1
     * return -> 날짜의 배열. 각 날짜는 UTC 밀리초
     */
    public long[] expand(Time dtstart,
                         RecurrenceSet recur,
                         long rangeStartMillis,
                         long rangeEndMillis) throws DateException {
        String timezone = dtstart.timezone;
        mIterator.clear(timezone);
        mGenerated.clear(timezone);

        // We don't need to clear the mUntil (and it wouldn't do any good to
        // do so) because the "until" date string is specified in UTC and that
        // sets the timezone in the mUntil Time object.
        // "until" 날짜 문자열이 UTC에 지정되어 있고 그것이 mUntil 시간 객체의 시간대를 설정하기 때문에 mUntil을 지우지 않아야 함

        mIterator.set(rangeStartMillis);
        long rangeStartDateValue = normDateTimeComparisonValue(mIterator);

        long rangeEndDateValue;
        if (rangeEndMillis != -1) {
            mIterator.set(rangeEndMillis);
            rangeEndDateValue = normDateTimeComparisonValue(mIterator);
        } else {
            rangeEndDateValue = Long.MAX_VALUE;
        }

        TreeSet<Long> dtSet = new TreeSet<Long>();

        if (recur.rrules != null) {
            for (EventRecurrence rrule : recur.rrules) {
                expand(dtstart, rrule, rangeStartDateValue,
                        rangeEndDateValue, true /* add */, dtSet);
            }
        }
        if (recur.rdates != null) {
            for (long dt : recur.rdates) {
                // The dates are stored as milliseconds. We need to convert
                // them to year/month/day values in the local timezone.
                // 날짜는 밀리초 단위로 저장되기 때문에 년/월/일 값으로 변환해야함
                mIterator.set(dt);
                long dtvalue = normDateTimeComparisonValue(mIterator);
                dtSet.add(dtvalue);
            }
        }
        if (recur.exrules != null) {
            for (EventRecurrence exrule : recur.exrules) {
                expand(dtstart, exrule, rangeStartDateValue,
                        rangeEndDateValue, false /* remove */, dtSet);
            }
        }
        if (recur.exdates != null) {
            for (long dt : recur.exdates) {
                // The dates are stored as milliseconds. We need to convert
                // them to year/month/day values in the local timezone.
                mIterator.set(dt);
                long dtvalue = normDateTimeComparisonValue(mIterator);
                dtSet.remove(dtvalue);
            }
        }
        if (dtSet.isEmpty()) {
            // this can happen if the recurrence does not occur within the
            // expansion window.
            // 만약 확장 창 안에서 일정 반복이 일어나지 않을 경우
            return new long[0];
        }

        // The values in dtSet are represented in a special form that is useful
        // for fast comparisons and that is easy to generate from year/month/day
        // values. We need to convert these to UTC milliseconds and also to
        // ensure that the dates are valid.
        // dtSet의 값은 빠른 비교에 유용하고, 년/월/일 값에서 생성하기 쉬운 특수한 형태로 표현.
        // 이를 UTC 밀리초로 변환해야 하고, 또한 날짜가 유효한지 확인해야함.
        int len = dtSet.size();
        long[] dates = new long[len];
        int i = 0;
        for (Long val: dtSet) {
            setTimeFromLongValue(mIterator, val);
            dates[i++] = mIterator.toMillis(true /* ignore isDst */);
        }
        return dates;
    }

    /**
     * Run the recurrence algorithm.  Processes events defined in the local
     * timezone of the event.  Return a list of iCalendar DATETIME
     * strings containing the start date/times of the occurrences; the output
     * times are defined in the local timezone of the event.
     *
     * 반복 알고리즘 실행. 이벤트의 로컬 시간대에 정의된 이벤트 처리
     * 이벤트 발생의 시작 날짜/시간을 포함하는 iCalendat DATETIME 문자열 목록 반환
     * 출력 시간은 이벤트의 로컬 시간대에 정의
     *
     * If you want all of the events, pass Long.MAX_VALUE for rangeEndDateValue.  If you pass
     * Long.MAX_VALUE for rangeEnd, and the event doesn't have a COUNT or UNTIL field,
     * you'll get a DateException.
     *
     * 모든 이벤트를 원
     *
     * 이벤트에 COUNT 또는 UNTIL 필드가 없으면 예외 발생
     *
     * @param dtstart the dtstart date as defined in RFC2445.  This
     * {@link Time} should be in the timezone of the event.
     * @param r the parsed recurrence, as defiend in RFC2445
     * @param rangeStartDateValue the first date-time you care about, inclusive
     * @param rangeEndDateValue the last date-time you care about, not inclusive (so
     *                  if you care about everything up through and including
     *                  Dec 22 1995, set last to Dec 23, 1995 00:00:00
     * @param add Whether or not we should add to out, or remove from out.
     * @param out the TreeSet you'd like to fill with the events
     * @throws DateException
     * @throws android.util.TimeFormatException if r cannot be parsed.
     *
     * dtstart -> RFC2445에 정의된 것과 같은 dtstart 날짜. 이벤트의 시간대에 있어야함
     * r -> RFC2445의 파싱된 반복
     * rangeStartValue -> 일정의 첫 날짜와 시간
     * rangeEndDateValue -> 일정의 마지막 날짜와 시간
     * add -> 추가할지 제거할지 여부
     * out -> 이벤트로 채워질 TreeSet
     */
    public void expand(Time dtstart,
                       EventRecurrence r,
                       long rangeStartDateValue,
                       long rangeEndDateValue,
                       boolean add,
                       TreeSet<Long> out) throws DateException {
        unsafeNormalize(dtstart);
        long dtstartDateValue = normDateTimeComparisonValue(dtstart);
        int count = 0;

        // add the dtstart instance to the recurrence, if within range.
        // For example, if dtstart is Mar 1, 2010 and the range is Jan 1 - Apr 1,
        // then add it here and increment count.  If the range is earlier or later,
        // then don't add it here.  In that case, count will be incremented later
        // inside  the loop. It is important that count gets incremented exactly
        // once here or in the loop for dtstart.
        //
        // NOTE: if DTSTART is not synchronized with the recurrence rule, the first instance
        //       we return will not fit the RRULE pattern.
        // 범위 내에 있는 경우 dtstart 인스턴스를 반복에 추가
        // 예를 들어, dtstart가 3월 1일이고 범위는 1월 1일부터 4월 1일이라면,
        // 이를 추가하고 count를 증가시킴.
        // 범위가 이전 또는 이후인 경우, 추가하지 않음.
        // 그런 경우에 count는 나중에 루프 내부에서 증가될 것.
        // dtstart의 경우 이곳 또는 루프에서 정확히 한번 count가 증가되는 것이 중요
        //
        // 참고 : DTSTART가 반복 규칙과 동기화되지 않은 경우, 반환되는 첫번째 인스턴스는 RRULE 패턴에 맞지 않음
        if (add && dtstartDateValue >= rangeStartDateValue
                && dtstartDateValue < rangeEndDateValue) {
            out.add(dtstartDateValue);
            ++count;
        }

        Time iterator = mIterator;
        Time until = mUntil;
        StringBuilder sb = mStringBuilder;
        Time generated = mGenerated;
        DaySet days = mDays;

        try {

            days.setRecurrence(r);
            if (rangeEndDateValue == Long.MAX_VALUE && r.until == null && r.count == 0) {
                throw new DateException(
                        "No range end provided for a recurrence that has no UNTIL or COUNT.");
            }

            // the top-level frequency
            int freqField;
            int freqAmount = r.interval;
            int freq = r.freq;
            switch (freq)
            {
                case EventRecurrence.SECONDLY:
                    freqField = Time.SECOND;
                    break;
                case EventRecurrence.MINUTELY:
                    freqField = Time.MINUTE;
                    break;
                case EventRecurrence.HOURLY:
                    freqField = Time.HOUR;
                    break;
                case EventRecurrence.DAILY:
                    freqField = Time.MONTH_DAY;
                    break;
                case EventRecurrence.WEEKLY:
                    freqField = Time.MONTH_DAY;
                    freqAmount = 7 * r.interval;
                    if (freqAmount <= 0) {
                        freqAmount = 7;
                    }
                    break;
                case EventRecurrence.MONTHLY:
                    freqField = Time.MONTH;
                    break;
                case EventRecurrence.YEARLY:
                    freqField = Time.YEAR;
                    break;
                default:
                    throw new DateException("bad freq=" + freq);
            }
            if (freqAmount <= 0) {
                freqAmount = 1;
            }

            int bymonthCount = r.bymonthCount;
            boolean usebymonth = useBYX(freq, EventRecurrence.MONTHLY, bymonthCount);
            boolean useDays = freq >= EventRecurrence.WEEKLY &&
                    (r.bydayCount > 0 || r.bymonthdayCount > 0);
            int byhourCount = r.byhourCount;
            boolean usebyhour = useBYX(freq, EventRecurrence.HOURLY, byhourCount);
            int byminuteCount = r.byminuteCount;
            boolean usebyminute = useBYX(freq, EventRecurrence.MINUTELY, byminuteCount);
            int bysecondCount = r.bysecondCount;
            boolean usebysecond = useBYX(freq, EventRecurrence.SECONDLY, bysecondCount);

            // initialize the iterator
            iterator.set(dtstart);
            if (freqField == Time.MONTH) {
                if (useDays) {
                    // if it's monthly, and we're going to be generating
                    // days, set the iterator day field to 1 because sometimes
                    // we'll skip months if it's greater than 28.
                    // XXX Do we generate days for MONTHLY w/ BYHOUR?  If so,
                    // we need to do this then too.
                    // 월 단위일 경우 일을 생성하려면 iterator의 일 필드를 1로 설정
                    // 28보다 클 경우 몇 달을 건너뛸 수 있기 때문
                    iterator.monthDay = 1;
                }
            }

            long untilDateValue;
            if (r.until != null) {
                // Ensure that the "until" date string is specified in UTC.
                // "until" 문자열이 UTC에 지정되어 있는지 확인
                String untilStr = r.until;
                // 15 is length of date-time without trailing Z e.g. "20090204T075959"
                // A string such as 20090204 is a valid UNTIL (see RFC 2445) and the
                // Z should not be added.
                // 15는 Z를 따라가지 않는 날짜 시간의 길이. 예) "20090204T075959"
                // 20090204와 같은 문자열은 유효한 UNTIL이며(RFC 2445 참조) Z를 추가해서는 안됨.
                if (untilStr.length() == 15) {
                    untilStr = untilStr + 'Z';
                }
                // The parse() method will set the timezone to UTC
                // parse() 메소드는 시간대를 UTC로 설정
                until.parse(untilStr);

                // We need the "until" year/month/day values to be in the same
                // timezone as all the generated dates so that we can compare them
                // using the values returned by normDateTimeComparisonValue().
                // normDateTimeComparisonValue()에서 반환된 값을 사용하여 비교할 수 있도록
                // 생성된 모든 날짜와 동일한 시간대에 "until" 년/월/일 값이 필요
                until.switchTimezone(dtstart.timezone);
                untilDateValue = normDateTimeComparisonValue(until);
            } else {
                untilDateValue = Long.MAX_VALUE;
            }

            sb.ensureCapacity(15);
            sb.setLength(15); // TODO: pay attention to whether or not the event
            // is an all-day one.

            if (SPEW) {
                Log.i(TAG, "expand called w/ rangeStart=" + rangeStartDateValue
                        + " rangeEnd=" + rangeEndDateValue);
            }

            // go until the end of the range or we're done with this event
            // 범위가 끝날 때까지 진행하거나 이 이벤트를 완료
            boolean eventEnded = false;
            int failsafe = 0; // Avoid infinite loops
            events: {
                while (true) {
                    int monthIndex = 0;
                    if (failsafe++ > MAX_ALLOWED_ITERATIONS) { // Give up after about 1 second of processing
                        Log.w(TAG, "Recurrence processing stuck with r=" + r + " rangeStart="
                                + rangeStartDateValue + " rangeEnd=" + rangeEndDateValue);
                        break;
                    }

                    unsafeNormalize(iterator);

                    int iteratorYear = iterator.year;
                    int iteratorMonth = iterator.month + 1;
                    int iteratorDay = iterator.monthDay;
                    int iteratorHour = iterator.hour;
                    int iteratorMinute = iterator.minute;
                    int iteratorSecond = iterator.second;

                    // year is never expanded -- there is no BYYEAR
                    generated.set(iterator);

                    if (SPEW) Log.i(TAG, "year=" + generated.year);

                    do { // month
                        int month = usebymonth
                                ? r.bymonth[monthIndex]
                                : iteratorMonth;
                        month--;
                        if (SPEW) Log.i(TAG, "  month=" + month);

                        int dayIndex = 1;
                        int lastDayToExamine = 0;

                        // Use this to handle weeks that overlap the end of the month.
                        // Keep the year and month that days is for, and generate it
                        // when needed in the loop
                        // 월말과 겹치는 주를 처리하기 위해 이 항목 사용
                        // 해당 일수를 보관하고 루프에 필요할 때 생성
                        if (useDays) {
                            // Determine where to start and end, don't worry if this happens
                            // to be before dtstart or after the end, because that will be
                            // filtered in the inner loop
                            // 어디서 시작하고 언제 끝날 지 결정
                            // dtstart 이전인지 이후인지 신경쓸 필요 없음
                            // 내부 루프에서 필터링될 것이기 때문
                            if (freq == EventRecurrence.WEEKLY) {
                                /*
                                 * iterator.weekDay indicates the day of the week (0-6, SU-SA).
                                 * Because dayIndex might start in the middle of a week, and we're
                                 * interested in treating a week as a unit, we want to move
                                 * backward to the start of the week.  (This could make the
                                 * dayIndex negative, which will be corrected by normalization
                                 * later on.)
                                 *
                                 * The day that starts the week is determined by WKST, which
                                 * defaults to MO.
                                 *
                                 * Example: dayIndex is Tuesday the 8th, and weeks start on
                                 * Thursdays.  Tuesday is day 2, Thursday is day 4, so we
                                 * want to move back (2 - 4 + 7) % 7 = 5 days to the previous
                                 * Thursday.  If weeks started on Mondays, we would only
                                 * need to move back (2 - 1 + 7) % 7 = 1 day.
                                 *
                                 * iterator.weekDay는 요일(0-6, SU-SA)을 나타냄
                                 * dayIndax는 일주일 중반부터 시작될 수 있고
                                 * 우리는 한 주를 하나의 단위로 취급하는 것에 관심이 있기 때문에
                                 * 한 주의 시작으로 되돌아가는 것이 좋음
                                 * (이는 dayIndax를 부정적으로 만들 수 있으며, 이것은 후에 정상화될 것임)
                                 *
                                 * 한 주를 시작하는 날은 WKST에 의해 결정되며, 기본값은 MO
                                 *
                                 * 예시 ) dayIndax는 8일 화요일이고, 몇 주는 목요일에 시작.
                                 * 화요일은 둘째 날이고 목요일은 넷째 날이기 때문에
                                 * (2 - 4 + 7) % 7 = 5일을 이전 목요일로 되돌리기를 원함
                                 * 만약 월요일부터 몇 주가 시작된다면,
                                 * (2 - 1 + 7) % 7 = 1일만 뒤로 물러나면 됨
                                 */
                                int weekStartAdj = (iterator.weekDay -
                                        EventRecurrence.day2TimeDay(r.wkst) + 7) % 7;
                                dayIndex = iterator.monthDay - weekStartAdj;
                                lastDayToExamine = dayIndex + 6;
                            } else {
                                lastDayToExamine = generated
                                        .getActualMaximum(Time.MONTH_DAY);
                            }
                            if (SPEW) Log.i(TAG, "dayIndex=" + dayIndex
                                    + " lastDayToExamine=" + lastDayToExamine
                                    + " days=" + days);
                        }

                        do { // day
                            int day;
                            if (useDays) {
                                if (!days.get(iterator, dayIndex)) {
                                    dayIndex++;
                                    continue;
                                } else {
                                    day = dayIndex;
                                }
                            } else {
                                day = iteratorDay;
                            }
                            if (SPEW) Log.i(TAG, "    day=" + day);

                            // hour
                            int hourIndex = 0;
                            do {
                                int hour = usebyhour
                                        ? r.byhour[hourIndex]
                                        : iteratorHour;
                                if (SPEW) Log.i(TAG, "      hour=" + hour + " usebyhour=" + usebyhour);

                                // minute
                                int minuteIndex = 0;
                                do {
                                    int minute = usebyminute
                                            ? r.byminute[minuteIndex]
                                            : iteratorMinute;
                                    if (SPEW) Log.i(TAG, "        minute=" + minute);

                                    // second
                                    int secondIndex = 0;
                                    do {
                                        int second = usebysecond
                                                ? r.bysecond[secondIndex]
                                                : iteratorSecond;
                                        if (SPEW) Log.i(TAG, "          second=" + second);

                                        // we do this here each time, because if we distribute it, we find the
                                        // month advancing extra times, as we set the month to the 32nd, 33rd, etc.
                                        // days.
                                        // 여기서 매번 이렇게 하는 이유는 배포를 하면 그 달을 32일, 33일 등으로 정함에 따라
                                        // 달이 추가로 앞당겨지는 것을 발견하기 때문
                                        generated.set(second, minute, hour, day, month, iteratorYear);
                                        unsafeNormalize(generated);

                                        long genDateValue = normDateTimeComparisonValue(generated);
                                        // sometimes events get generated (BYDAY, BYHOUR, etc.) that
                                        // are before dtstart.  Filter these.  I believe this is correct,
                                        // but Google Calendar doesn't seem to always do this.
                                        if (genDateValue >= dtstartDateValue) {
                                            // filter and then add
                                            // TODO: we don't check for stop conditions (like
                                            //       passing the "end" date) unless the filter
                                            //       allows the event.  Could stop sooner.
                                            int filtered = filter(r, generated);
                                            if (0 == filtered) {

                                                // increase the count as long
                                                // as this isn't the same
                                                // as the first instance
                                                // specified by the DTSTART
                                                // (for RRULEs -- additive).
                                                // This condition must be the complement of the
                                                // condition for incrementing count at the
                                                // beginning of the method, so if we don't
                                                // increment count there, we increment it here.
                                                // For example, if add is set and dtstartDateValue
                                                // is inside the start/end range, then it was added
                                                // and count was incremented at the beginning.
                                                // If dtstartDateValue is outside the range or add
                                                // is not set, then we must increment count here.
                                                // DTSTART에서 지정한 첫번째 인스턴스(RRULEs -- Additive)와 같지 않은 한 count 증가
                                                // 이 조건은 메소드 시작 시 count를 증가시키기 위한 조건의 보완이 되어야 하므로
                                                // 거기서 count를 증가시키지 않으면 여기에서 증가시킴
                                                // 예를 들어, 추가를 설정하고 dtstartDateValue가 시작/종료 범위 내에 있는 경우,
                                                // 추가되고 초기에 count가 증가.
                                                // dtstartDateValue가 범위를 벗어나거나 추가가 설정되지 않은 경우
                                                // 여기서 count를 증가시켜야 함
                                                if (!(dtstartDateValue == genDateValue
                                                        && add
                                                        && dtstartDateValue >= rangeStartDateValue
                                                        && dtstartDateValue < rangeEndDateValue)) {
                                                    ++count;
                                                }
                                                // one reason we can stop is that
                                                // we're past the until date
                                                // 멈출 수 있는 한 가지 이유는 날짜가 지났기 때문
                                                if (genDateValue > untilDateValue) {
                                                    if (SPEW) {
                                                        Log.i(TAG, "stopping b/c until="
                                                                + untilDateValue
                                                                + " generated="
                                                                + genDateValue);
                                                    }
                                                    break events;
                                                }
                                                // or we're past rangeEnd
                                                // 또는 범위를 벗어남
                                                if (genDateValue >= rangeEndDateValue) {
                                                    if (SPEW) {
                                                        Log.i(TAG, "stopping b/c rangeEnd="
                                                                + rangeEndDateValue
                                                                + " generated=" + generated);
                                                    }
                                                    break events;
                                                }

                                                if (genDateValue >= rangeStartDateValue) {
                                                    if (SPEW) {
                                                        Log.i(TAG, "adding date=" + generated + " filtered=" + filtered);
                                                    }
                                                    if (add) {
                                                        out.add(genDateValue);
                                                    } else {
                                                        out.remove(genDateValue);
                                                    }
                                                }
                                                // another is that count is high enough
                                                // 또 다른 것은 count가 충분히 높다는 것
                                                if (r.count > 0 && r.count == count) {
                                                    //Log.i(TAG, "stopping b/c count=" + count);
                                                    break events;
                                                }
                                            }
                                        }
                                        secondIndex++;
                                    } while (usebysecond && secondIndex < bysecondCount);
                                    minuteIndex++;
                                } while (usebyminute && minuteIndex < byminuteCount);
                                hourIndex++;
                            } while (usebyhour && hourIndex < byhourCount);
                            dayIndex++;
                        } while (useDays && dayIndex <= lastDayToExamine);
                        monthIndex++;
                    } while (usebymonth && monthIndex < bymonthCount);

                    // Add freqAmount to freqField until we get another date that we want.
                    // We don't want to "generate" dates with the iterator.
                    // XXX: We do this for days, because there is a varying number of days
                    // per month
                    // 원하는 다른 날짜를 얻을 때까지 freqField에 freqAmount를 추가
                    int oldDay = iterator.monthDay;
                    generated.set(iterator);  // just using generated as a temporary. 임시 생성
                    int n = 1;
                    while (true) {
                        int value = freqAmount * n;
                        switch (freqField) {
                            case Time.SECOND:
                                iterator.second += value;
                                break;
                            case Time.MINUTE:
                                iterator.minute += value;
                                break;
                            case Time.HOUR:
                                iterator.hour += value;
                                break;
                            case Time.MONTH_DAY:
                                iterator.monthDay += value;
                                break;
                            case Time.MONTH:
                                iterator.month += value;
                                break;
                            case Time.YEAR:
                                iterator.year += value;
                                break;
                            case Time.WEEK_DAY:
                                iterator.monthDay += value;
                                break;
                            case Time.YEAR_DAY:
                                iterator.monthDay += value;
                                break;
                            default:
                                throw new RuntimeException("bad field=" + freqField);
                        }

                        unsafeNormalize(iterator);
                        if (freqField != Time.YEAR && freqField != Time.MONTH) {
                            break;
                        }
                        if (iterator.monthDay == oldDay) {
                            break;
                        }
                        n++;
                        iterator.set(generated);
                    }
                }
            }
        }
        catch (DateException e) {
            Log.w(TAG, "DateException with r=" + r + " rangeStart=" + rangeStartDateValue
                    + " rangeEnd=" + rangeEndDateValue);
            throw e;
        }
        catch (RuntimeException t) {
            Log.w(TAG, "RuntimeException with r=" + r + " rangeStart=" + rangeStartDateValue
                    + " rangeEnd=" + rangeEndDateValue);
            throw t;
        }
    }

    /**
     * Normalizes the date fields to give a valid date, but if the time falls
     * in the invalid window during a transition out of Daylight Saving Time
     * when time jumps forward an hour, then the "normalized" value will be
     * invalid.
     * <p>
     * This method also computes the weekDay and yearDay fields.
     *
     * <p>
     * This method does not modify the fields isDst, or gmtOff.
     *
     * 유효한 날짜를 지정하기 위해 날짜 필드를 정규화 하지만,
     * 시간이 1시간 앞으로 점프할 때 Daylight Saving Time 초과 전환 중에 시간이 유효하지 않은 창에 떨어지면
     * 정규화된 값이 무효가 됨
     */
    static void unsafeNormalize(Time date) {
        int second = date.second;
        int minute = date.minute;
        int hour = date.hour;
        int monthDay = date.monthDay;
        int month = date.month;
        int year = date.year;

        int addMinutes = ((second < 0) ? (second - 59) : second) / 60;
        second -= addMinutes * 60;
        minute += addMinutes;
        int addHours = ((minute < 0) ? (minute - 59) : minute) / 60;
        minute -= addHours * 60;
        hour += addHours;
        int addDays = ((hour < 0) ? (hour - 23) : hour) / 24;
        hour -= addDays * 24;
        monthDay += addDays;

        // We want to make "monthDay" positive. We do this by subtracting one
        // from the year and adding a year's worth of days to "monthDay" in
        // the following loop while "monthDay" <= 0.
        // "monthDay"를 양수로
        // "monthDay"<=0 인 경우 다음 루프의 "monthDay"에 1년 치의 일수를 더함
        while (monthDay <= 0) {
            // If month is after Feb, then add this year's length so that we
            // include this year's leap day, if any.
            // Otherwise (the month is Feb or earlier), add last year's length.
            // Subtract one from the year in either case. This gives the same
            // effective date but makes monthDay (the day of the month) much
            // larger. Eventually (usually in one iteration) monthDay will
            // be positive.
            // 만약 2월 이후의 달이라면 올해의 윤년을 포함하도록 올해 길이를 더함
            // 그렇지 않은 경우(그 달은 2월 또는 그 이전) 작년 길이를 더함
            // 어느 경우든 1년을 뺌
            // 이는 같은 시행일을 주지만 monthDay 월요일은 훨씬 더 커짐
            // 결국 (보톤 한번의 반복에서) monyhDay가 양수가 됨
            int days = month > 1 ? yearLength(year) : yearLength(year - 1);
            monthDay += days;
            year -= 1;
        }
        // At this point, monthDay >= 1. Normalize the month to the range [0,11].
        // 이 시점에서 monthDay >= 1. 월을 범위 [0,11]로 표준화.
        if (month < 0) {
            int years = (month + 1) / 12 - 1;
            year += years;
            month -= 12 * years;
        } else if (month >= 12) {
            int years = month / 12;
            year += years;
            month -= 12 * years;
        }
        // At this point, month is in the range [0,11] and monthDay >= 1.
        // Now loop until the monthDay is in the correct range for the month.
        // 이 시점에서 월은 [0,11] 및 monthDay >= 1 범위에 있음
        // monthDay가 해당 월의 올바른 범위에 있을 때까지 순환
        while (true) {
            // On January, check if we can jump forward a whole year.
            // 1월에 1년 내내 앞으로 뛸 수 있는지 확인 ?
            if (month == 0) {
                int yearLength = yearLength(year);
                if (monthDay > yearLength) {
                    year++;
                    monthDay -= yearLength;
                }
            }
            int monthLength = monthLength(year, month);
            if (monthDay > monthLength) {
                monthDay -= monthLength;
                month++;
                if (month >= 12) {
                    month -= 12;
                    year++;
                }
            } else break;
        }
        // At this point, monthDay <= the length of the current month and is
        // in the range [1,31].
        // 이 시점에서 monthDay <= 현재 달의 길이 또는 [1,31] 범위에 있다.

        date.second = second;
        date.minute = minute;
        date.hour = hour;
        date.monthDay = monthDay;
        date.month = month;
        date.year = year;
        date.weekDay = weekDay(year, month, monthDay);
        date.yearDay = yearDay(year, month, monthDay);
    }

    /**
     * Returns true if the given year is a leap year.
     *
     * @param year the given year to test
     * @return true if the given year is a leap year.
     *
     * 주어진 연도가 윤년일 경우 true 반환
     * year -> 테스트할 특정 연도
     */
    static boolean isLeapYear(int year) {
        return (year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0));
    }

    /**
     * Returns the number of days in the given year.
     *
     * @param year the given year
     * @return the number of days in the given year.
     *
     * 주어진 연도의 일수 반환
     * year -> 주어진 연도
     */
    static int yearLength(int year) {
        return isLeapYear(year) ? 366 : 365;
    }

    private static final int[] DAYS_PER_MONTH = { 31, 28, 31, 30, 31, 30, 31,
            31, 30, 31, 30, 31 };
    private static final int[] DAYS_IN_YEAR_PRECEDING_MONTH = { 0, 31, 59, 90,
            120, 151, 180, 212, 243, 273, 304, 334 };

    /**
     * Returns the number of days in the given month of the given year.
     *
     * @param year the given year.
     * @param month the given month in the range [0,11]
     * @return the number of days in the given month of the given year.
     *
     * 주어진 연도의 주어진 달의 일수 반환
     * year -> 주어진 연도
     * month -> [0,11]의 범위를 가지는 주어진 달
     */
    static int monthLength(int year, int month) {
        int n = DAYS_PER_MONTH[month];
        if (n != 28) {
            return n;
        }
        return isLeapYear(year) ? 29 : 28;
    }

    /**
     * Computes the weekday, a number in the range [0,6] where Sunday=0, from
     * the given year, month, and day.
     *
     * @param year the year
     * @param month the 0-based month in the range [0,11]
     * @param day the 1-based day of the month in the range [1,31]
     * @return the weekday, a number in the range [0,6] where Sunday=0
     *
     * 일요일을 0으로 하고 범위가 [0,6]인 주어진 연도, 달, 날의 요일의 숫자를 계산
     * year -> 주어진 연도
     * month -> [0,11]을 범위로 가지고 0을 기준으로 하는 주어진 달
     * day -> [1,31]을 범위로 가지고 1을 기준으로 하는 주어진 날
     * return -> 일요일을 0으로 하는 범위 [0,6]의 숫자
     */
    static int weekDay(int year, int month, int day) {
        if (month <= 1) {
            month += 12;
            year -= 1;
        }
        return (day + (13 * month - 14) / 5 + year + year/4 - year/100 + year/400) % 7;
    }

    /**
     * Computes the 0-based "year day", given the year, month, and day.
     *
     * @param year the year
     * @param month the 0-based month in the range [0,11]
     * @param day the 1-based day in the range [1,31]
     * @return the 0-based "year day", the number of days into the year
     *
     * 연도, 월, 일 단위로 0을 기준으로 하여 "year day 연간" 계산
     * year -> 연도
     * month -> [0,11]을 범위로 가지고 0을 기준으로 하는 주어진 달
     * day -> [1,31]을 범위로 가지고 1을 기준으로 하는 주어진 날
     * return -> 0을 기준으로 하는 "year day 연간" 일. 일수.
     */
    static int yearDay(int year, int month, int day) {
        int yearDay = DAYS_IN_YEAR_PRECEDING_MONTH[month] + day - 1;
        if (month >= 2 && isLeapYear(year)) {
            yearDay += 1;
        }
        return yearDay;
    }

    /**
     * Converts a normalized Time value to a 64-bit long. The mapping of Time
     * values to longs provides a total ordering on the Time values so that
     * two Time values can be compared efficiently by comparing their 64-bit
     * long values.  This is faster than converting the Time values to UTC
     * millliseconds.
     *
     * @param normalized a Time object whose date and time fields have been
     * normalized
     * @return a 64-bit long value that can be used for comparing and ordering
     * dates and times represented by Time objects
     *
     * 정규화된 시간 값을 64비트 길이로 변환.
     * 긴 값에 대한 시간 값의 매핑은 64비트의 긴 값을 비교하여 두 개의 시간 값을 효율적으로 비교할 수 있도록
     * 시간 값에 대한 전체 순서를 제공
     * 이는 시간 값을 UTC 밀리세컨드로 변환하는 것보다 더 빠름
     */
    private static final long normDateTimeComparisonValue(Time normalized) {
        // 37 bits for the year, 4 bits for the month, 5 bits for the monthDay,
        // 5 bits for the hour, 6 bits for the minute, 6 bits for the second.
        // 연도에 37비트, 달에 4비트, monthDay에 5비트
        // 시간당 5비트, 분당 6비트, 초당 6비트
        return ((long)normalized.year << 26) + (normalized.month << 22)
                + (normalized.monthDay << 17) + (normalized.hour << 12)
                + (normalized.minute << 6) + normalized.second;
    }

    private static final void setTimeFromLongValue(Time date, long val) {
        date.year = (int) (val >> 26);
        date.month = (int) (val >> 22) & 0xf;
        date.monthDay = (int) (val >> 17) & 0x1f;
        date.hour = (int) (val >> 12) & 0x1f;
        date.minute = (int) (val >> 6) & 0x3f;
        date.second = (int) (val & 0x3f);
    }
}
