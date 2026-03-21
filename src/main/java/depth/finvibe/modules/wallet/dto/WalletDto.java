package depth.finvibe.modules.wallet.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import depth.finvibe.modules.wallet.domain.Wallet;

public class WalletDto {

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class WalletResponse {
        private Long walletId;
        private UUID userId;
        private Long balance;

        public static WalletResponse from(Wallet wallet) {
            return WalletResponse.builder()
                    .walletId(wallet.getId())
                    .userId(wallet.getUserId())
                    .balance(wallet.getBalance().getPrice())
                    .build();
        }
    }
}
