package com.resale.resalecontentmanagement.components.cBank;

import com.resale.resalecontentmanagement.components.cBank.dto.BankKeyDetailsResponseDTO;
import com.resale.resalecontentmanagement.components.cBank.dto.BankKeyRequestDTO;
import com.resale.resalecontentmanagement.components.cBank.dto.BankKeyResponseDTO;
import com.resale.resalecontentmanagement.components.cBank.dto.BanksResponseDTO;
import com.resale.resalecontentmanagement.model.CBank;
import com.resale.resalecontentmanagement.model.CBankKeys;
import com.resale.resalecontentmanagement.model.Project;
import com.resale.resalecontentmanagement.repository.ProjectRepository;
import com.resale.resalecontentmanagement.repository.CBankKeysRepository;
import com.resale.resalecontentmanagement.repository.CBankRepository;
import com.resale.resalecontentmanagement.utils.ReturnObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CBankService {

    private final CBankKeysRepository bankKeysRepository;
    private final CBankRepository cBankRepository;
    private final ProjectRepository projectRepository;

    public ReturnObject<List<BankKeyResponseDTO>> getAllBankKeys() {

        ReturnObject<List<BankKeyResponseDTO>> response = new ReturnObject<>();

        List<BankKeyResponseDTO> result =
                bankKeysRepository.findAllBankKeysWithDetails();

        response.setStatus(true);
        response.setMessage("Bank keys retrieved successfully");
        response.setData(result);

        return response;
    }

    public ReturnObject<List<BanksResponseDTO>> getAllAvailableBanks() {

        ReturnObject<List<BanksResponseDTO>> response = new ReturnObject<>();

        List<CBank> banksList =
                cBankRepository.findAll();

        List<BanksResponseDTO> banksResponseDTOList = new ArrayList<>();
        for(CBank cBank : banksList){
            banksResponseDTOList.add(new BanksResponseDTO(cBank.getId(), cBank.getBankName()));
        }

        response.setStatus(true);
        response.setMessage("Bank keys retrieved successfully");
        response.setData(banksResponseDTOList);

        return response;
    }


    public ReturnObject<BankKeyDetailsResponseDTO> getBankKeyById(Integer id) {

        ReturnObject<BankKeyDetailsResponseDTO> response = new ReturnObject<>();

        BankKeyDetailsResponseDTO result =
                bankKeysRepository.findBankKeyDetailsById(id);

        if (result == null) {
            response.setStatus(false);
            response.setMessage("Bank key not found");
            response.setData(null);
            return response;
        }

        response.setStatus(true);
        response.setMessage("Bank key retrieved successfully");
        response.setData(result);

        return response;
    }

    public ReturnObject<List<BankKeyResponseDTO>> createBankKeysPerProject(Long userId,BankKeyRequestDTO bankKeyRequestDTO) {
        ReturnObject returnObject = new ReturnObject();
        Optional<Project> project = projectRepository.findById(bankKeyRequestDTO.getProjectId());
        if(!project.isPresent()){
            returnObject.setMessage("No Project found with this Id");
            returnObject.setStatus(false);
            returnObject.setData(null);
        }
        CBankKeys cBankKeysExists = bankKeysRepository.findCBankKeysByProjectId(bankKeyRequestDTO.getProjectId());
        if(cBankKeysExists != null){
            returnObject.setMessage("Bank Key with same project already exists");
            returnObject.setStatus(false);
            returnObject.setData(null);
        }
        try {
            CBankKeys cBankKeys = new CBankKeys();
            cBankKeys.setAccessKey(bankKeyRequestDTO.getAccessKey());
            cBankKeys.setBankId(bankKeyRequestDTO.getBankId());
            cBankKeys.setProjectId(bankKeyRequestDTO.getProjectId());
            cBankKeys.setProfileId(bankKeyRequestDTO.getProfileId());
            cBankKeys.setExtraKey(bankKeyRequestDTO.getExtraKey());
            cBankKeys.setSecretKey(bankKeyRequestDTO.getSecretKey());
            cBankKeys.setLastChangeUserId(userId);
            cBankKeys.setCreatedAt(LocalDateTime.now());
            cBankKeys.setUpdatedAt(LocalDateTime.now());
            cBankKeys = bankKeysRepository.save(cBankKeys);
            returnObject.setMessage("Added Bank Keys Successfully");
            returnObject.setStatus(true);
            returnObject.setData(cBankKeys);
        }catch (Exception exception) {
            returnObject.setMessage("Couldn't add Bank Key for this project");
            returnObject.setStatus(false);
            returnObject.setData(null);
        }
        return returnObject;
    }
    public ReturnObject<List<?>> updateBankKeysPerProject(Long userId, int projectId , BankKeyRequestDTO bankKeyRequestDTO) {
        CBankKeys cBankKeys = bankKeysRepository.findCBankKeysByProjectId(projectId);
        ReturnObject returnObject = new ReturnObject();
        if(cBankKeys != null) {
            cBankKeys.setAccessKey(bankKeyRequestDTO.getAccessKey());
            cBankKeys.setBankId(bankKeyRequestDTO.getBankId());
            cBankKeys.setProjectId(projectId);
            cBankKeys.setProfileId(bankKeyRequestDTO.getProfileId());
            cBankKeys.setExtraKey(bankKeyRequestDTO.getExtraKey());
            cBankKeys.setSecretKey(bankKeyRequestDTO.getSecretKey());
            cBankKeys.setLastChangeUserId(userId);
            cBankKeys = bankKeysRepository.save(cBankKeys);
            returnObject.setData(cBankKeys);
            returnObject.setMessage("Updated Successfully");
            returnObject.setStatus(true);
        }else{
            returnObject.setStatus(false);
            returnObject.setData(null);
            returnObject.setMessage("Couldn't find bank in configurations");
        }
        return returnObject;

    }
}


