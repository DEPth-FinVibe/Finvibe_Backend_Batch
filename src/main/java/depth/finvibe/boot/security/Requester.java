package depth.finvibe.boot.security;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import depth.finvibe.modules.user.domain.enums.UserRole;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Requester {

	private UUID userId;
	private UserRole role;

	public UUID getUuid() {
		return userId;
	}
}
