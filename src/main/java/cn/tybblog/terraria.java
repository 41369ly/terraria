package cn.tybblog;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class terraria {
    public static void main(String[] args) {
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();
            JobDetail jobDetail = JobBuilder.newJob(UpdateJob.class)
                    .withIdentity("job1", "group1").build();
            Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger1", "triggerGroup1")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInHours(1)
                            .repeatForever()).build();
            scheduler.scheduleJob(jobDetail, trigger);
            scheduler.start();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

}
