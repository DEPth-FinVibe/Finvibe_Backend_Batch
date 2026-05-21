package depth.finvibe.boot.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import depth.finvibe.modules.user.domain.enums.UserRole;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Requester {

	private Long userId;
	private UserRole role;

	public Long getUuid() {
		return userId;
	}
}
