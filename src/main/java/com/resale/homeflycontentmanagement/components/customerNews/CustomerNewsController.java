package com.resale.homeflycontentmanagement.components.customerNews;

import com.resale.homeflycontentmanagement.components.customerNews.dto.NewsResponseDTO;
import com.resale.homeflycontentmanagement.utils.PaginatedResponseDTO;
import com.resale.homeflycontentmanagement.utils.ReturnObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerNewsController {

    @Autowired
    CustomerNewsService customerNewsService;

    @GetMapping("/news")
    public ReturnObject<PaginatedResponseDTO<NewsResponseDTO>> getActiveNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return customerNewsService.getActiveNews(page, size);
    }

    @GetMapping("/news/{id}")
    public ReturnObject<NewsResponseDTO> getNewsById(@PathVariable Integer id) {
        return customerNewsService.getNewsById(id);
    }

}

