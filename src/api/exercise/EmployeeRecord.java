package exercise;

import java.time.LocalDate;

public record EmployeeRecord(
        int empId,
        int projectId,
        LocalDate dateFrom,
        LocalDate dateTo
) {}