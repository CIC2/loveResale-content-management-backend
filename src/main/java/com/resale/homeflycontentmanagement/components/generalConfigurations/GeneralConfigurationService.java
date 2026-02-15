package com.resale.homeflycontentmanagement.components.generalConfigurations;

import com.resale.homeflycontentmanagement.components.generalConfigurations.dto.AddConfigDTO;
import com.resale.homeflycontentmanagement.components.generalConfigurations.dto.GetConfigDTO;
import com.resale.homeflycontentmanagement.components.generalConfigurations.dto.UpdateConfigDTO;
import com.resale.homeflycontentmanagement.model.Configurations;
import com.resale.homeflycontentmanagement.repository.ConfigurationsRepository;
import com.resale.homeflycontentmanagement.utils.ReturnObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GeneralConfigurationService {

    @Autowired
    private ConfigurationsRepository configurationsRepository;

    public ReturnObject createConfiguration(Long userId, AddConfigDTO addConfigDTO) {
        ReturnObject returnObject = new ReturnObject();
        try {
            Configurations configration = new Configurations(userId, addConfigDTO);
            configurationsRepository.save(configration);
            returnObject.setData(addConfigDTO);
            returnObject.setMessage("Configurations Added Successfully");
            returnObject.setStatus(true);
        } catch (Exception exception) {
            System.out.println("Exception : " + exception.getMessage());
            returnObject.setStatus(false);
            returnObject.setMessage("Error while Adding Configurations");
            returnObject.setData(addConfigDTO);
        }
        return returnObject;
    }

    public ReturnObject updateConfiguration(Long userId, Integer configId, UpdateConfigDTO updateConfigDTO) {
        ReturnObject returnObject = new ReturnObject();
        try {
            Optional<Configurations> configrationOptional = configurationsRepository.findById(configId);
            if (configrationOptional.isPresent()) {
                Configurations configuration = configrationOptional.get();
                updateConfigurationInfo(userId, configuration, updateConfigDTO);
                configurationsRepository.save(configuration);
                returnObject.setData(updateConfigDTO);
                returnObject.setMessage("Configurations Updated Successfully");
                returnObject.setStatus(true);
            } else {
                System.out.println("Exception Not Found: ");
                returnObject.setStatus(false);
                returnObject.setMessage("Couldn't find Config");
                returnObject.setData(updateConfigDTO);
            }
        } catch (Exception exception) {
            System.out.println("Exception : " + exception.getMessage());
            returnObject.setStatus(false);
            returnObject.setMessage("Error while Updating Configurations");
            returnObject.setData(updateConfigDTO);
        }
        return returnObject;
    }


    public ReturnObject findAllConfigurations(Long userId) {
        ReturnObject returnObject = new ReturnObject();
        try {
            List<GetConfigDTO> getConfigDTOList = new ArrayList<>();
            List<Configurations> configurationList = configurationsRepository.findAll();
            for (Configurations configurations : configurationList) {
                getConfigDTOList.add(new GetConfigDTO(configurations));
            }
            returnObject.setData(getConfigDTOList);
            returnObject.setStatus(true);
            returnObject.setMessage("Fetched Successfully");
        } catch (Exception exception) {
            returnObject.setData(null);
            returnObject.setStatus(false);
            returnObject.setMessage("Fetched Successfully");
        }
        return returnObject;
    }

    public ReturnObject updateAllConfigurations(Long userId, List<UpdateConfigDTO> updateConfigDTO) {
        ReturnObject returnObject = new ReturnObject();
        try {
            for (UpdateConfigDTO configDTO : updateConfigDTO) {
                Optional<Configurations> configrationOptional = configurationsRepository.findById(configDTO.getId());
                Configurations configuration = configrationOptional.get();
                if (configrationOptional.isPresent()) {
                    updateConfigurationInfo(userId, configuration, configDTO);
                    configurationsRepository.save(configuration);
                    returnObject.setData(updateConfigDTO);
                    returnObject.setMessage("Configurations Updated Successfully");
                    returnObject.setStatus(true);
                } else {
                    System.out.println("Exception Not Found: ");
                }
            }
        } catch (Exception exception) {
            System.out.println("Exception : " + exception.getMessage());
            returnObject.setStatus(false);
            returnObject.setMessage("Error while Updating Configurations");
            returnObject.setData(updateConfigDTO);
        }
        return returnObject;
    }

    private void updateConfigurationInfo(Long userId, Configurations configuration, UpdateConfigDTO updateConfigDTO) {
        configuration.setDescription(updateConfigDTO.getDescription());
        configuration.setConfigKey(updateConfigDTO.getConfigKey());
        configuration.setConfigValue(updateConfigDTO.getConfigValue());
        configuration.setConditionType(updateConfigDTO.getConditionType());
        configuration.setConditionValue(updateConfigDTO.getConditionValue());
        configuration.setLastModifiedBy(userId);
    }


}


