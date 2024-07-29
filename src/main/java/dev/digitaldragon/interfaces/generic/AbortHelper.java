package dev.digitaldragon.interfaces.generic;

import dev.digitaldragon.jobs.JobManager;

public class AbortHelper {
    /**
     * Powers abort commands.
     *
     * @param jobId The ID of the job to be aborted. May be invalid.
     * @return A user-friendly message about whether the job was aborted.
     */
    public static String abortJob(String jobId) {
        if (JobManager.abort(jobId))
            return "Đã hủy bỏ tác vụ " + jobId + "!";
        else
            return "Không thể hủy bỏ tác vụ " + jobId + "! Tác vụ không tồn tại, hoặc nó đã bị hủy sẵn rồi.";
    }
}
