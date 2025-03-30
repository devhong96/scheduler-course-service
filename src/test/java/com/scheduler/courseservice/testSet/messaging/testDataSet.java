package com.scheduler.courseservice.testSet.messaging;

import org.junit.jupiter.api.TestInstance;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class testDataSet {

    public final static String token = "Bearer test-token";
    public final static int mockYear = LocalDate.now().getYear();
    public final static int mockWeek = LocalDate.now().get(WeekFields.of(Locale.getDefault()).weekOfYear());

}
