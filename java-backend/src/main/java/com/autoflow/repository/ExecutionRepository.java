package com.autoflow.repository;
import com.autoflow.model.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ExecutionRepository extends JpaRepository<Execution,Long> {
  List<Execution> findTop50ByOrderByStartedAtDesc();
}
