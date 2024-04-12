package irt.components.beans;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;

public enum UserRoles implements GrantedAuthority{

	ADD_RMA					(512		, "old role: 'WORK_ORDER'; Add RMA Unit by serial number."),
	ADD_RMA_COMMENT			(1024		, "old role: 'SCAN_LOG_FILE'; Add comment to RMA Unit"),
	USER_EDIT			(2048		, "Can edit IRT worker's data."),
	SHIPPING				(4096		, "old role: 'SCHEMATIC_LETTER'; "),
    NEW_PART_NUMBER			(8192		, "old role: 'ALT_PART_NUMBER'; Create new part number."),
    ADD_INVENTORY_TRANSFER	(16384		, "old role: 'EDIT_COST'; Can add Inventory Transfer"),
    CUSTOMER_ORDER		(32768		, "Can edit CUSTOMER ORDERs"),
    SCHEMATIC_PART		(65536		, "Can change schematic part(AutoCAD)."),
    ECO					(131072		, "old role: 'STOCK'; The User can create ESOs"),
    EDIT_COMPANIES		(262144		, "Can add, edit, delete company"),
    USER				(524288		, "Can see all IRT workers"),
	ADMIN				(1048576	, "Admin"),
	WIP_PAGE			(2097152	, "old role: 'EDITING'; Can access to the VIP page."),
	SELLERS				(4194304	, "Can add, edit, delete sellers"),
	CALIBRATION_SETTINGS	(8388608	, "old role: 'DATABASE'; Save Calibration Settings"),
	EDIT_BOM			(16777216	, "Can edit BOMs."),
	STOCK_REPORT		(33554432	, "Download Component report to CSV file."),
	ENGINEERING			(67108864	, "Engineering Department."),
	ENGINEERING_TOP		(134217728	, "May approve the ECO."),
	ENGINEERING_ECO		(268435456	, "Cen creat new Engineering Change Request."),
	ENGINEERING_BCC		(536870912	, "Final approval.");

	private final long permission;
	private String description;

	private UserRoles(long permission, String description) {
		this.permission = permission;
		this.description = description;
	}

	public static Collection<? extends GrantedAuthority> getAuthorities(long permissions) {

		UserRoles[] values = values();
		return Arrays.stream(values).filter(ur->(ur.permission&permissions)!=0).collect(Collectors.toList());
	}

	public long getPermission() {
		return permission;
	}

	public String getDescription() {
		return description;
	}

	public boolean hasRole(Long roles) {
		return Optional.ofNullable(roles).map(r->r&permission).map(r->r>0).orElse(false);
	}

	public static Long toLong(UserRoles[] userRoles) {
		return Arrays.stream(userRoles).parallel().mapToLong(UserRoles::getPermission).sum();
	}

	@Override
	public String getAuthority() {
		return name();
	}
}
