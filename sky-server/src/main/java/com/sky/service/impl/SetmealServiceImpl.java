package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐及对应的菜品
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDishes(SetmealDTO setmealDTO) {
        //插入套餐基本信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        setmealMapper.insert(setmeal);

        //获取insert回填的套餐id
        Long setmealId = setmeal.getId();

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes != null && setmealDishes.size() > 0){
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
            //批量插入套餐内菜品信息
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    /**
     * 套餐分页查询
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());

        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);

        long total = page.getTotal();
        List<SetmealVO> records = page.getResult();

        return new PageResult(total,records);
    }

    /**
     * 根据id删除套餐数据
     * @param ids
     */
    @Transactional
    public void deleteById(List<Long> ids) {

        //如果还在售，就不能删
        for(Long id : ids){
            Setmeal setmeal = setmealMapper.getById(id);
            if(setmeal.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //批量删除：优化性能
        setmealMapper.deleteByIds(ids);
    }

    /**
     * 根据id查询套餐信息
     * @param id
     * @return
     */
    public SetmealVO getById(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);

        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }
}
