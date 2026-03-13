package com.example.demoworkflow.repository;

import com.example.demoworkflow.pojo.CFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CFileRepository extends JpaRepository<CFile, Long> {
    CFile getCFilesByWorkspaceAndUuid(String workspace, String uuid);
    List<CFile> getCFilesByWorkspace(String workspace);
    void deleteAllById(long id);
}
