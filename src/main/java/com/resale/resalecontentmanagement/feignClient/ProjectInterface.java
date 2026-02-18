package com.resale.resalecontentmanagement.feignClient;

import com.resale.resalecontentmanagement.components.objectstorage.dto.ProjectDTO;
import com.resale.resalecontentmanagement.utils.ReturnObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "ProjectApp" ,
             url = "${inventory.service.url}"  )
public interface ProjectInterface {
    @GetMapping("/project")
    ReturnObject<List<ProjectDTO>> getAllProjects();
}


