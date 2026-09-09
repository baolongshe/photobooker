package com.xhxi.photobooker.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xhxi.photobooker.entity.Address;
import com.xhxi.photobooker.mapper.AddressMapper;
import com.xhxi.photobooker.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/address")
public class AddressController {
    @Autowired
    private AddressMapper addressMapper;

    // 获取用户所有地址
    @GetMapping("/{userId}")
    public Result<List<Address>>getUserAddresses(@PathVariable Long userId) {
        return Result.success(addressMapper.selectList(new QueryWrapper<Address>().eq("user_id", userId)));
    }

    // 添加地址
    @PostMapping()
    public Result<Boolean> addAddress(@RequestBody Address address) {
        addressMapper.insert(address);
        return Result.success(true);
    }

    // 删除地址
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteAddress(@PathVariable Long id) {
        addressMapper.deleteById(id);
        return Result.success(true);
    }

    // 设置默认地址
    @PostMapping("/default")
    public Result<Boolean> setDefaultAddress(@RequestParam Long userId, @RequestParam Long addressId) {
        // 先将该用户所有地址isDefault设为0
        addressMapper.update(null, new UpdateWrapper<Address>().eq("user_id", userId).set("is_default", 0));
        // 再将目标地址isDefault设为1
        Address address = addressMapper.selectById(addressId);
        if (address != null) {
            address.setIsDefault(1);
            addressMapper.updateById(address);
        }
        return Result.success(true);
    }
}
