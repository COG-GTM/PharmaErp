package com.pharma.erp.staff.service;

import com.pharma.erp.common.web.ConflictException;
import com.pharma.erp.common.web.NotFoundException;
import com.pharma.erp.common.web.PageResponse;
import com.pharma.erp.staff.dto.StaffRequest;
import com.pharma.erp.staff.dto.StaffResponse;
import com.pharma.erp.staff.entity.Staff;
import com.pharma.erp.staff.entity.StaffType;
import com.pharma.erp.staff.repository.LoginTypeRepository;
import com.pharma.erp.staff.repository.StaffRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffService {

    private static final Set<String> SORTABLE = Set.of(
            "id", "firstName", "lastName", "joinDate", "dateOfBirth", "salary", "email", "staffType", "createdAt");

    private final StaffRepository staff;
    private final LoginTypeRepository loginTypes;

    public StaffService(StaffRepository staff, LoginTypeRepository loginTypes) {
        this.staff = staff;
        this.loginTypes = loginTypes;
    }

    /** Filters for {@link #list}; every field is optional. */
    public record StaffFilter(String q, Integer loginTypeId, Integer storeId, StaffType staffType, Boolean active) {
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> list(StaffFilter filter, Pageable pageable) {
        Pageable page = Pageables.withAllowedSort(pageable, SORTABLE, Sort.by("id"));
        return PageResponse.from(staff.findAll(toSpecification(filter), page), StaffResponse::from);
    }

    @Transactional(readOnly = true)
    public StaffResponse get(Integer id) {
        return StaffResponse.from(staff.findWithLoginTypeById(id).orElseThrow(() -> NotFoundException.of("Staff", id)));
    }

    @Transactional
    public StaffResponse create(StaffRequest request) {
        Staff entity = new Staff();
        apply(entity, request, null);
        return StaffResponse.from(staff.saveAndFlush(entity));
    }

    @Transactional
    public StaffResponse update(Integer id, StaffRequest request) {
        Staff entity = find(id);
        apply(entity, request, id);
        return StaffResponse.from(staff.saveAndFlush(entity));
    }

    @Transactional
    public void delete(Integer id) {
        staff.delete(find(id));
        staff.flush();
    }

    Staff find(Integer id) {
        return staff.findById(id).orElseThrow(() -> NotFoundException.of("Staff", id));
    }

    private void apply(Staff entity, StaffRequest r, Integer currentId) {
        String email = Pageables.trimToNull(r.email());
        if (email != null) {
            email = email.toLowerCase(Locale.ROOT);
            boolean taken = currentId == null
                    ? staff.existsByEmailIgnoreCase(email)
                    : staff.existsByEmailIgnoreCaseAndIdNot(email, currentId);
            if (taken) {
                throw new ConflictException("A staff member with email '" + email + "' already exists");
            }
        }
        String username = Pageables.trimToNull(r.username());
        if (username != null) {
            boolean taken = currentId == null
                    ? staff.existsByUsername(username)
                    : staff.existsByUsernameAndIdNot(username, currentId);
            if (taken) {
                throw new ConflictException("Username '" + username + "' is already linked to another staff member");
            }
            if (staff.countUsers(username) == 0) {
                throw new IllegalArgumentException("username '" + username + "' does not exist");
            }
        }
        if (r.storeId() != null && staff.countStores(r.storeId()) == 0) {
            throw new IllegalArgumentException("storeId " + r.storeId() + " does not exist");
        }
        entity.setLoginType(r.loginTypeId() == null
                ? null
                : loginTypes.findById(r.loginTypeId()).orElseThrow(() ->
                        new IllegalArgumentException("loginTypeId " + r.loginTypeId() + " does not exist")));

        entity.setFirstName(r.firstName().trim());
        entity.setMiddleName(Pageables.trimToNull(r.middleName()));
        entity.setLastName(Pageables.trimToNull(r.lastName()));
        entity.setDateOfBirth(r.dateOfBirth());
        entity.setJoinDate(r.joinDate());
        entity.setExperience(Pageables.trimToNull(r.experience()));
        entity.setSalary(r.salary());
        entity.setHomeTown(Pageables.trimToNull(r.homeTown()));
        entity.setHomeTownAddress(Pageables.trimToNull(r.homeTownAddress()));
        entity.setJobAddress(Pageables.trimToNull(r.jobAddress()));
        entity.setLeavingAddress(Pageables.trimToNull(r.leavingAddress()));
        entity.setMobileNumber(Pageables.trimToNull(r.mobileNumber()));
        entity.setEmail(email);
        entity.setLastCompany(Pageables.trimToNull(r.lastCompany()));
        entity.setResumePath(Pageables.trimToNull(r.resumePath()));
        entity.setFeedback(Pageables.trimToNull(r.feedback()));
        entity.setSuggestion(Pageables.trimToNull(r.suggestion()));
        entity.setUsername(username);
        entity.setStoreId(r.storeId());
        entity.setStaffType(r.staffType() == null ? StaffType.INTERNAL : r.staffType());
        entity.setActive(r.active() == null || r.active());
    }

    static Specification<Staff> toSpecification(StaffFilter filter) {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("loginType", JoinType.LEFT);
            }
            List<Predicate> predicates = new ArrayList<>();
            String q = Pageables.trimToNull(filter.q());
            if (q != null) {
                String like = "%" + q.toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), like, '\\'),
                        cb.like(cb.lower(root.get("middleName")), like, '\\'),
                        cb.like(cb.lower(root.get("lastName")), like, '\\'),
                        cb.like(cb.lower(root.get("email")), like, '\\'),
                        cb.like(cb.lower(root.get("username")), like, '\\')));
            }
            if (filter.loginTypeId() != null) {
                predicates.add(cb.equal(root.get("loginType").get("id"), filter.loginTypeId()));
            }
            if (filter.storeId() != null) {
                predicates.add(cb.equal(root.get("storeId"), filter.storeId()));
            }
            if (filter.staffType() != null) {
                predicates.add(cb.equal(root.get("staffType"), filter.staffType()));
            }
            if (filter.active() != null) {
                predicates.add(cb.equal(root.get("active"), filter.active()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
