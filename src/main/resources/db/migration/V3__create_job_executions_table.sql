CREATE TABLE job_executions (
                                id UUID PRIMARY KEY,
                                job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
                                status VARCHAR(20) NOT NULL,
                                attempt INT NOT NULL,
                                started_at TIMESTAMP NOT NULL,
                                finished_at TIMESTAMP,
                                duration_ms BIGINT,
                                error_message TEXT,
                                created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_job_executions_job_id ON job_executions(job_id);
