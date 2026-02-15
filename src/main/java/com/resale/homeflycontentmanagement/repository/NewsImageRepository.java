package com.resale.homeflycontentmanagement.repository;

import com.resale.homeflycontentmanagement.model.NewsImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NewsImageRepository extends JpaRepository<NewsImage, Integer> {

 List<NewsImage> findByNewsId(Integer newsId);



    List<NewsImage> findByNewsIdIn(List<Integer> ids);

    @Modifying
    @Query("DELETE FROM NewsImage i WHERE i.id IN :deletedIds")
    void deleteByIds(@Param("deletedIds") List<Integer> deletedIds);

    @Modifying
    @Query("DELETE FROM NewsImage i WHERE i.id IN :deletedIds AND i.newsId =:newsId")
    void deleteAllByIdInAndNewsId(List<Integer> deletedIds, Integer newsId);

    int countByNewsId(Integer newsId);
}


