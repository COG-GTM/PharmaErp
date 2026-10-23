package com.pharma.erp.stores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharma.erp.common.web.ConflictException;
import com.pharma.erp.common.web.NotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class StoreServiceTest {

    private StoreRepository repository;
    private StaffDirectory staff;
    private StoreService service;

    @BeforeEach
    void setUp() {
        repository = mock(StoreRepository.class);
        staff = mock(StaffDirectory.class);
        service = new StoreService(repository, staff);
        when(repository.saveAndFlush(any(Store.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createNormalisesCodeGstinAndBlanks() {
        StoreResponse created = service.create(
                new StoreRequest(" mum-02 ", "  Mumbai West ", "   ", "27aabcp1234f1z5", null));

        assertThat(created.code()).isEqualTo("MUM-02");
        assertThat(created.name()).isEqualTo("Mumbai West");
        assertThat(created.address()).isNull();
        assertThat(created.gstin()).isEqualTo("27AABCP1234F1Z5");
        assertThat(created.managerStaffId()).isNull();
    }

    @Test
    void createTreatsBlankGstinAsNull() {
        assertThat(service.create(new StoreRequest("AB", "Store AB", "addr", "", null)).gstin()).isNull();
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.existsByCode("MUM-01")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new StoreRequest("mum-01", "Other", null, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("code 'MUM-01'");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsDuplicateName() {
        when(repository.existsByName("Mumbai")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new StoreRequest("NEW-1", "Mumbai", null, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("name 'Mumbai'");
    }

    @Test
    void createRejectsUnknownManager() {
        when(staff.exists(42)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new StoreRequest("NEW-1", "New", null, null, 42)))
                .isInstanceOf(InvalidReferenceException.class)
                .extracting("field").isEqualTo("managerStaffId");
    }

    @Test
    void createAcceptsExistingManager() {
        when(staff.exists(7)).thenReturn(true);

        assertThat(service.create(new StoreRequest("NEW-1", "New", null, null, 7)).managerStaffId()).isEqualTo(7);
    }

    @Test
    void updateOfMissingStoreIsNotFound() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99, new StoreRequest("X1", "X", null, null, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Store 99 not found");
    }

    @Test
    void updateAllowsKeepingOwnCodeButNotAnothers() {
        Store existing = new Store();
        when(repository.findById(5)).thenReturn(Optional.of(existing));
        when(repository.existsByCodeAndIdNot("TAKEN", 5)).thenReturn(true);

        assertThat(service.update(5, new StoreRequest("own", "Own", null, null, null)).code()).isEqualTo("OWN");
        assertThatThrownBy(() -> service.update(5, new StoreRequest("taken", "Own", null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void listRejectsUnknownSortProperty() {
        assertThatThrownBy(() -> service.list(null, PageRequest.of(0, 10, Sort.by("password"))))
                .isInstanceOf(InvalidReferenceException.class)
                .extracting("field").isEqualTo("sort");
        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void deleteOfReferencedStoreIsConflict() {
        Store existing = new Store();
        when(repository.findById(3)).thenReturn(Optional.of(existing));
        doThrow(new DataIntegrityViolationException("fk")).when(repository).flush();

        assertThatThrownBy(() -> service.delete(3))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("still referenced");
    }
}
