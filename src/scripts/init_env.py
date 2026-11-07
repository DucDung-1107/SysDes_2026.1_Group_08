"""Create local-only Compose settings without overwriting an existing file."""

import argparse
import os
from pathlib import Path
import secrets


def create_env(destination: Path, api_port: int, web_port: int) -> Path:
    if api_port == web_port or not all(1 <= port <= 65535 for port in (api_port, web_port)):
        raise ValueError("Choose distinct API/frontend ports between 1 and 65535.")
    destination = destination.expanduser().resolve()
    if not destination.parent.is_dir():
        raise ValueError("The destination directory must already exist.")
    content = (
        f"API_PORT={api_port}\nWEB_PORT={web_port}\n"
        "POSTGRES_DB=medora\nPOSTGRES_USER=medora\n"
        f"POSTGRES_PASSWORD={secrets.token_hex(24)}\n"
        "SPRING_PROFILES_ACTIVE=dev\n"
        f"MEDORA_BOOTSTRAP_PASSWORD={secrets.token_urlsafe(24)}\n"
        "SESSION_COOKIE_SECURE=false\n"
        "MEDORA_FACILITY_ZONE=Asia/Ho_Chi_Minh\n"
        "MEDORA_WALK_IN_ENABLED=true\nNEXT_PUBLIC_FACILITY_NAMES={}\n"
        "MEDORA_STORAGE_DRIVER=filesystem\n"
    )
    # O_EXCL prevents a race from replacing a configuration or changing passwords.
    descriptor = os.open(destination, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as output:
        output.write(content)
    return destination


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=Path(__file__).resolve().parents[1] / ".env")
    parser.add_argument("--api-port", type=int, default=8080)
    parser.add_argument("--web-port", type=int, default=3000)
    args = parser.parse_args()
    try:
        path = create_env(args.output, args.api_port, args.web_port)
    except FileExistsError:
        parser.exit(1, "Configuration already exists; keeping it unchanged. Use another --output for an isolated environment.\n")
    except (OSError, ValueError) as error:
        parser.exit(1, f"Could not create configuration: {error}\n")
    print(f"Created {path}. Passwords are stored only in that ignored local file.")


if __name__ == "__main__":
    main()
