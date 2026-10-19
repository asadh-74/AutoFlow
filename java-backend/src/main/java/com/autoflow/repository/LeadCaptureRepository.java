package com.autoflow.repository;
import com.autoflow.model.LeadCapture;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface LeadCaptureRepository extends JpaRepository<LeadCapture,String>{List<LeadCapture> findTop50ByOrderByCreatedAtDesc();}
