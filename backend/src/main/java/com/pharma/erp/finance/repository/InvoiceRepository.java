package com.pharma.erp.finance.repository;

import com.pharma.erp.finance.entity.Invoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer>, JpaSpecificationExecutor<Invoice> {

    @Query("select distinct i from Invoice i left join fetch i.lines where i.id = :id")
    Optional<Invoice> findWithLinesById(@Param("id") Integer id);

    @Query("""
            select new com.pharma.erp.finance.repository.InvoiceAggregate(
                count(i), sum(i.subtotal), sum(i.gstAmount), sum(i.totalAmount), sum(i.paidAmount))
            from Invoice i
            where i.status <> 'CANCELLED' and i.invoiceDate between :from and :to
            """)
    InvoiceAggregate aggregate(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select new com.pharma.erp.finance.repository.GstRateAggregate(
                l.gstRate, sum(l.lineTotal - l.gstAmount), sum(l.gstAmount))
            from InvoiceLine l join l.invoice i
            where i.status <> 'CANCELLED' and i.invoiceDate between :from and :to
            group by l.gstRate
            order by l.gstRate
            """)
    List<GstRateAggregate> gstByRate(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
