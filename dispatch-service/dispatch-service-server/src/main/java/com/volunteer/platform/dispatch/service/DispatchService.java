package com.volunteer.platform.dispatch.service;

import com.volunteer.platform.dispatch.dto.ExecuteDispatchDTO;
import com.volunteer.platform.dispatch.dto.DetectShortageDTO;
import com.volunteer.platform.dispatch.vo.DispatchResultVO;

import java.util.List;

public interface DispatchService {

    DispatchResultVO execute(ExecuteDispatchDTO dto);

    List<DispatchResultVO> detectShortage(DetectShortageDTO dto);

    DispatchResultVO getResult(Long id);
}
