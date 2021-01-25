package com.baidu.shop.entity;

/**
 * @ClassName CategoryBrandEntity
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/1/23
 * @Version V1.0
 **/

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Table;

@Table(name = "tb_category_brand")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBrandEntity {

    private Integer categoryId;

    private Integer brandId;
}
