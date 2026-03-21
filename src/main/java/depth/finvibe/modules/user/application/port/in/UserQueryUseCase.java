package depth.finvibe.modules.user.application.port.in;

import java.util.UUID;

public interface UserQueryUseCase {
    String getNickname(UUID userId);
}
