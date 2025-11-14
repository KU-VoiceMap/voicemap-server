import datetime
from google import genai
from google.genai import types
import os
import sys
import argparse

def create_token(api_key, uses, expire_mins, session_expire_mins):
    try:
        os.environ['GOOGLE_API_KEY'] = api_key

        client = genai.Client(
            http_options={'api_version': 'v1alpha'}
        )

        now = datetime.datetime.now(tz=datetime.timezone.utc)
        token_config = {
            'uses': uses,
            'expire_time': now + datetime.timedelta(minutes=expire_mins),
            'new_session_expire_time': now + datetime.timedelta(minutes=session_expire_mins),
            'http_options': {'api_version': 'v1alpha'},
        }

        token = client.auth_tokens.create(config=token_config)

        print(token.name)

    except Exception as e:
        print(f"Error: {str(e)}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    
    parser.add_argument("--api-key", type=str, required=True, help="Google API 키")
    parser.add_argument("--uses", type=int, required=True, help="토큰 사용 횟수")
    parser.add_argument("--expire-mins", type=int, required=True, help="토큰 만료 시간 (분)")
    parser.add_argument("--session-expire-mins", type=int, required=True, help="세션 만료 시간 (분)")

    args = parser.parse_args()

    create_token(args.api_key, args.uses, args.expire_mins, args.session_expire_mins)