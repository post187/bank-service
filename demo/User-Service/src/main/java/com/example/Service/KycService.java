package com.example.Service;

import com.example.Model.Dto.External.KycAiResultEvent;
import com.example.Model.Dto.Internal.Status.KycStatus;
import com.example.Model.Dto.Request.UpdateUserKyc;
import com.example.Model.Dto.Response.Response;
import com.example.Model.Dto.Response.UserKycDtoUser;

import java.util.List;

public interface KycService {
    UserKycDtoUser submitKyc(UpdateUserKyc request);

    List<UserKycDtoUser> getMyKycHistory();

    UserKycDtoUser getLatestKyc();

    void processAiResult(KycAiResultEvent resultEvent);

    Response updateKycStatus(Long kycId, KycStatus status, String reason);

}
