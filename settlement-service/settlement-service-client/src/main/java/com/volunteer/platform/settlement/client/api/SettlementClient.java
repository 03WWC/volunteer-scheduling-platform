package com.volunteer.platform.settlement.client.api;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.settlement.client.dto.GenerateSettlementDTO;
import com.volunteer.platform.settlement.client.dto.PaySettlementDTO;
import com.volunteer.platform.settlement.client.dto.SettlementBillDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "settlement-service")
public interface SettlementClient {

    @PostMapping("/settlement/generate")
    Result<SettlementBillDTO> generate(@RequestBody GenerateSettlementDTO dto);

    @GetMapping("/settlement/{id}/detail")
    Result<SettlementBillDTO> getDetail(@PathVariable("id") Long id);

    @GetMapping("/settlement/users/{userId}/bills")
    Result<List<SettlementBillDTO>> listByUser(@PathVariable("userId") Long userId);

    @PostMapping("/settlement/{id}/confirm")
    Result<SettlementBillDTO> confirm(@PathVariable("id") Long id);

    @PostMapping("/settlement/{id}/pay")
    Result<SettlementBillDTO> pay(@PathVariable("id") Long id, @RequestBody PaySettlementDTO dto);
}
