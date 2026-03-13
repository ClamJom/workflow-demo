package com.example.demoworkflow.mapper;

import com.example.demoworkflow.pojo.CFile;
import com.example.demoworkflow.vo.CFileVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CFileMapper {
    @Mapping(source="date", target="created")
    CFile cFileVoToCFile(CFileVO vo);
    @Mapping(source="created", target="date")
    CFileVO cFileToCFileVo(CFile cFile);
    @Mapping(source="created", target="date")
    List<CFileVO> cFileListToCFileVoList(List<CFile> cFile);
}
