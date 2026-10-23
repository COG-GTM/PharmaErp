package com.pharma.erp.finance.repository;

import com.pharma.erp.finance.entity.Expense;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Integer>, JpaSpecificationExecutor<Expense> {

    @Query("""
            select new com.pharma.erp.finance.repository.ExpenseCategoryAggregate(
                e.category, count(e), sum(e.amount), sum(e.gstAmount))
            from Expense e
            where e.status <> 'CANCELLED' and e.expenseDate between :from and :to
            group by e.category
            order by e.category
            """)
    List<ExpenseCategoryAggregate> aggregateByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
