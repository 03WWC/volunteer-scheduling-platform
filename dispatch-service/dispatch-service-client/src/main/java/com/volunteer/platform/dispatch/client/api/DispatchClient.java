package com.volunteer.platform.dispatch.client.api;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.dispatch.client.dto.DispatchDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "dispatch-service")
public interface DispatchClient {

    @PostMapping("/dispatch/execute")
    Result<DispatchDTO> execute(@RequestBody DispatchDTO dispatchDTO);

    @GetMapping("/dispatch/{id}/result")
    Result<DispatchDTO> getResult(@PathVariable("id") Long id);

    @PostMapping("/dispatch/detect-shortage")
    Result<List<DispatchDTO>> detectShortage(@RequestBody DispatchDTO dispatchDTO);
}
