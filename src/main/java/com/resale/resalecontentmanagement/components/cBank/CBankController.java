package com.resale.resalecontentmanagement.components.cBank;

import com.resale.resalecontentmanagement.components.cBank.dto.BankKeyDetailsResponseDTO;
import com.resale.resalecontentmanagement.components.cBank.dto.BankKeyRequestDTO;
import com.resale.resalecontentmanagement.components.cBank.dto.BankKeyResponseDTO;
import com.resale.resalecontentmanagement.components.cBank.dto.BanksResponseDTO;
import com.resale.resalecontentmanagement.security.user.CurrentUserId;
import com.resale.resalecontentmanagement.utils.ReturnObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bank")
@RequiredArgsConstructor
public class CBankController {

    private final CBankService cBankService;

    @GetMapping
    public ResponseEntity<ReturnObject<List<BankKeyResponseDTO>>> getAllBankKeys() {

        ReturnObject<List<BankKeyResponseDTO>> response =
                cBankService.getAllBankKeys();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
    @PutMapping("/{projectId}")
    public ResponseEntity<ReturnObject<?>> updateBankKeys(@CurrentUserId Long userId,@PathVariable int projectId ,@RequestBody BankKeyRequestDTO bankKeyRequestDTO) {

        ReturnObject<?> response =
                cBankService.updateBankKeysPerProject(userId,projectId,bankKeyRequestDTO);
        if(response.getStatus()) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(response);
        }else{
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }
    @PostMapping("")
    public ResponseEntity<ReturnObject<?>> createBankKeys(@CurrentUserId Long userId, @RequestBody BankKeyRequestDTO bankKeyRequestDTO) {

        ReturnObject<?> response =
                cBankService.createBankKeysPerProject(userId,bankKeyRequestDTO);
        if(response.getStatus()) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(response);
        }else{
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }
    @GetMapping("banksList")
    public ResponseEntity<ReturnObject<List<BanksResponseDTO>>> getAllBanks() {

        ReturnObject<List<BanksResponseDTO>> response =
                cBankService.getAllAvailableBanks();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReturnObject<BankKeyDetailsResponseDTO>> getBankKeyById(
            @PathVariable Integer id) {

        ReturnObject<BankKeyDetailsResponseDTO> response =
                cBankService.getBankKeyById(id);

        HttpStatus status = response.getStatus()
                ? HttpStatus.OK
                : HttpStatus.NOT_FOUND;

        return ResponseEntity
                .status(status)
                .body(response);
    }

}



