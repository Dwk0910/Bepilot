package org.dslofficial.bepilot;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class TurnOffNotifyJobService extends JobService {
    // Implement the job service to handle the turn off notification logic
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (MainActivity.e_warning_turnoff) {
            // Your logic to handle the turn off notification
            MainActivity.sendNotification(this, "스마트폰 지속 사용시간이 10분을 초과했습니다.", "과도한 스마트폰 사용은 시력과 안압에 문제를 초래할 수 있습니다. 스마트폰을 종료해 주십시오.");
        }
        return false; // Return true if the job is still running.
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
