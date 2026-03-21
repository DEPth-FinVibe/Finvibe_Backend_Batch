package depth.finvibe.modules.wallet.application;

import depth.finvibe.modules.wallet.application.port.in.WalletQueryUseCase;
import depth.finvibe.modules.wallet.application.port.out.WalletRepository;
import depth.finvibe.modules.wallet.domain.Wallet;
import depth.finvibe.modules.wallet.domain.error.WalletErrorCode;
import depth.finvibe.modules.wallet.dto.WalletDto;
import depth.finvibe.common.error.DomainException;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService implements WalletQueryUseCase {
    private final WalletRepository walletRepository;
    private final MeterRegistry meterRegistry;

    @Override
    public WalletDto.WalletResponse getWalletByUserId(UUID userId) {
        Wallet wallet = findWallet(userId);

        return WalletDto.WalletResponse.from(wallet);
    }

    private Wallet findWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    meterRegistry.counter("wallet.wallet.not_found").increment();
                    return new DomainException(WalletErrorCode.WALLET_NOT_FOUND);
                });
    }
}
