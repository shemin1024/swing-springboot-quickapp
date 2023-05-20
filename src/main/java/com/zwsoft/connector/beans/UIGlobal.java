package com.zwsoft.connector.beans;

import com.zwsoft.connector.enums.FrameState;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
public class UIGlobal {
    @Getter
    @Setter
    private FrameState frameState =FrameState.LOADING;
}
