package com.pharma.erp.staff.service;

import com.pharma.erp.common.web.NotFoundException;
import com.pharma.erp.common.web.PageResponse;
import com.pharma.erp.staff.dto.SalaryRequest;
import com.pharma.erp.staff.dto.SalaryResponse;
import com.pharma.erp.staff.entity.Salary;
import com.pharma.erp.staff.entity.Staff;
import com.pharma.erp.staff.repository.SalaryRepository;
import com.pharma.erp.staff.repository.StaffRepository;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalaryService {

    private static final Set<String> SORTABLE = Set.of("id", "amount", "payPeriod", "paidOn", "createdAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("payPeriod"), Sort.Order.desc("id"));

    private final SalaryRepository salaries;
    private final StaffRepository staff;

    public SalaryService(SalaryRepository salaries, StaffRepository staff) {
        this.salaries = salaries;
        this.staff = staff;
    }

    @Transactional(readOnly = true)
    public PageResponse<SalaryResponse> list(Integer staffId, Pageable pageable) {
        requireStaff(staffId);
        Pageable page = Pageables.withAllowedSort(pageable, SORTABLE, DEFAULT_SORT);
        return PageResponse.from(salaries.findByStaffId(staffId, page), s -> SalaryResponse.from(s, staffId));
    }

    @Transactional(readOnly = true)
    public SalaryResponse get(Integer staffId, Integer salaryId) {
        return SalaryResponse.from(find(staffId, salaryId), staffId);
    }

    @Transactional
    public SalaryResponse create(Integer staffId, SalaryRequest request) {
        Salary salary = new Salary(requireStaff(staffId));
        apply(salary, request);
        return SalaryResponse.from(salaries.saveAndFlush(salary), staffId);
    }

    @Transactional
    public SalaryResponse update(Integer staffId, Integer salaryId, SalaryRequest request) {
        Salary salary = find(staffId, salaryId);
        apply(salary, request);
        return SalaryResponse.from(salaries.saveAndFlush(salary), staffId);
    }

    @Transactional
    public void delete(Integer staffId, Integer salaryId) {
        salaries.delete(find(staffId, salaryId));
        salaries.flush();
    }

    private Staff requireStaff(Integer staffId) {
        return staff.findById(staffId).orElseThrow(() -> NotFoundException.of("Staff", staffId));
    }

    private Salary find(Integer staffId, Integer salaryId) {
        requireStaff(staffId);
        return salaries.findByIdAndStaffId(salaryId, staffId)
                .orElseThrow(() -> NotFoundException.of("Salary", salaryId));
    }

    private static void apply(Salary salary, SalaryRequest request) {
        salary.setAmount(request.amount());
        salary.setPayPeriod(Pageables.trimToNull(request.payPeriod()));
        salary.setPaidOn(request.paidOn());
    }
}
