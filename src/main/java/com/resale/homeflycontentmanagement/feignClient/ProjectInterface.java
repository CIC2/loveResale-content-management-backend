package com.resale.homeflycontentmanagement.feignClient;

import com.resale.homeflycontentmanagement.components.objectstorage.dto.ProjectDTO;
import com.resale.homeflycontentmanagement.utils.ReturnObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "ProjectApp" ,
             url = "${inventory.service.url}"  )
public interface ProjectInterface {
    @GetMapping("/project")
    ReturnObject<List<ProjectDTO>> getAllProjects();
}


