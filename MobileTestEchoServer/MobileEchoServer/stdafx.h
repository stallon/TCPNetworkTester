// stdafx.h : ���� ��������� ���� ��������� �ʴ�
// ǥ�� �ý��� ���� ���� �� ������Ʈ ���� ���� ������
// ��� �ִ� ���� �����Դϴ�.
//

#pragma once

#if defined(_WIN32) || defined(_WIN64)
#include "targetver.h"

#include <stdio.h>
#include <tchar.h>

#define WINAPI	__stdcall
#define MAIN	_tmain
#else
#define WINAPI
#define MAIN	main
#endif




// TODO: ���α׷��� �ʿ��� �߰� ����� ���⿡�� �����մϴ�.
