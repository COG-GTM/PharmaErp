package com.pharma.erp.staff.service;

import com.pharma.erp.common.web.ConflictException;
import com.pharma.erp.common.web.NotFoundException;
import com.pharma.erp.common.web.PageResponse;
import com.pharma.erp.staff.dto.LoginTypeRequest;
import com.pharma.erp.staff.dto.LoginTypeResponse;
import com.pharma.erp.staff.entity.LoginType;
import com.pharma.erp.staff.repository.LoginTypeRepository;
import com.pharma.erp.staff.repository.StaffRepository;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginTypeService {

    private static final Set<String> SORTABLE = Set.of("id", "name");

    private final LoginTypeRepository loginTypes;
    private final StaffRepository staff;

    public LoginTypeService(LoginTypeRepository loginTypes, StaffRepository staff) {
        this.loginTypes = loginTypes;
        this.staff = staff;
    }

    @Transactional(readOnly = true)
    public PageResponse<LoginTypeResponse> list(Pageable pageable) {
        Pageable page = Pageables.withAllowedSort(pageable, SORTABLE, Sort.by("id"));
        return PageResponse.from(loginTypes.findAll(page), LoginTypeResponse::from);
    }

    @Transactional(readOnly = true)
    public LoginTypeResponse get(Integer id) {
        return LoginTypeResponse.from(find(id));
    }

    @Transactional
    public LoginTypeResponse create(LoginTypeRequest request) {
        String name = request.name().trim();
        if (loginTypes.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Login type '" + name + "' already exists");
        }
        return LoginTypeResponse.from(loginTypes.saveAndFlush(new LoginType(name)));
    }

    @Transactional
    public LoginTypeResponse update(Integer id, LoginTypeRequest request) {
        LoginType loginType = find(id);
        String name = request.name().trim();
        if (loginTypes.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Login type '" + name + "' already exists");
        }
        loginType.setName(name);
        return LoginTypeResponse.from(loginTypes.saveAndFlush(loginType));
    }

    @Transactional
    public void delete(Integer id) {
        LoginType loginType = find(id);
        if (staff.existsByLoginTypeId(id)) {
            throw new ConflictException("Login type " + id + " is assigned to staff and cannot be deleted");
        }
        loginTypes.delete(loginType);
        loginTypes.flush();
    }

    LoginType find(Integer id) {
        return loginTypes.findById(id).orElseThrow(() -> NotFoundException.of("Login type", id));
    }
}
