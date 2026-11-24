# PetSpot 브랜드 가이드

> `TSK-DES-001` (2026-11-24). 디자인·문구·캐릭터 그림을 만들 때 이 문서를 기준으로 한다. 결정 배경은 DECISIONS.md ADR-015.

## 1. 브랜드 메시지
- **한 줄 소개**: 헛걸음 없는 반려동물 여행
- **캐릭터와 함께 쓸 때**: 헛걸음 없는 반려동물 여행, 콩이와 나비가 먼저 다녀왔어요.
- **두 가지 가치**: 설레는 탐색(콩이) + 믿을 수 있는 정보(나비)

## 2. 캐릭터

### 콩이 (메인)
| 항목 | 내용 |
|---|---|
| 종 | 웰시코기, 3살 (성별 정하지 않음) |
| 역할 | 여행 대장 — 탐색, 길 안내, 추천 |
| 성격 | 호기심 많고 낙천적. 일단 가 보고 생각한다. 실수해도 금방 털고 일어난다. |
| 생김새 | 앰버색 털, 이마에서 배까지 크림색, 몸에 비해 큰 세모 귀, 짧은 다리, 혀를 내민 웃는 얼굴 |
| 소품 | 민트색 반다나(발자국 무늬), 갈색 배낭, 지도, 카메라 |
| 말투 | 밝은 반말 — "여기 가 보자!", "킁킁, 냄새가 좋은데?" |
| 입버릇 | "일단 가 보자!" |
| 약점 | 길치. 지도를 거꾸로 본다. |

### 나비 (서브)
| 항목 | 내용 |
|---|---|
| 종 | 치즈 고양이(주황 줄무늬), 5살 (성별 정하지 않음) |
| 역할 | 깐깐한 평가단 — 리뷰, 별점, 동반 조건 확인 |
| 성격 | 신중하고 까다롭지만 정이 많다. 나비가 인정한 곳이면 믿을 만하다. |
| 생김새 | 주황 줄무늬 털, 크림색 입 주변과 배, 반쯤 감은 눈(기본 표정) |
| 소품 | 빨간 목걸이와 금색 방울, 돋보기, 메모장, 책 |
| 말투 | 차분한 존댓말 — "음… 주차장은 확인해 보셨나요?" |
| 입버릇 | "확인부터 해요." |
| 약점 | 낯선 곳에서는 처음에 꼭 숨는다. |

### 관계
콩이가 신나서 먼저 달려가면, 나비가 뒤따르며 "확인부터 해요"라고 챙긴다.

### 짧은 스토리 「콩이의 첫 번째 지도」
신나게 달려간 카페 앞에서 "반려동물 출입 금지" 팻말을 만나고, 도착한 펜션이 소형견만 받는다는 걸 그제야 알던 콩이. 나비가 돋보기를 들고 "확인부터 해요"라며 함께 지도를 펼쳤다. 대형견도 되는 곳, 테라스는 괜찮은 곳을 하나씩 짚어 발자국을 찍었고, 그날 콩이는 처음으로 바닷가를 마음껏 달렸다. 나비는 메모장에 별 다섯 개를 그렸다. "다음에 올 친구들도 알 수 있게요." 그 지도가 PetSpot이다.

## 3. 그림 규칙 (캐릭터 그림을 새로 만들 때 반드시 지킨다)
1. **콩이에게는 줄무늬가 없다.** 코는 검고 둥근 개 코, 입은 개 입. (이미지 AI가 콩이를 고양이처럼 그리는 경우가 잦다)
2. **소품을 섞지 않는다.** 배낭·지도·카메라는 콩이, 돋보기·메모장·책·방울 목걸이는 나비.
   - **예외 (2026-11-24)**: 둘이 함께 여행하는 장면에서는 나비도 배낭을 메고 지도를 들 수 있다 — 길치인 콩이 대신 나비가 지도를 확인하는 장면("확인부터 해요"). 홈 대표 그림 `duo-together`가 이 경우다.
3. **반다나에는 발자국 무늬만**, 글자를 넣지 않는다.
4. **그림 안에 글자를 넣지 않는다.** 문구는 화면에서 글자로 넣는다.
5. 스타일: 부드러운 털 질감의 3D 일러스트, 둥근 비율(큰 머리), 따뜻한 조명.
6. 파일: 512px 이상, **투명 배경 PNG**, 캐릭터 주변 여백 약간. 사이트에는 WebP로 변환해 넣는다(장당 80KB 이하 목표).
7. 48px 이하(파비콘·앱 아이콘)에는 털 질감 그림 대신 단순한 얼굴 아이콘을 쓴다.

## 4. 색
| 이름 | 값 | 쓰는 곳 |
|---|---|---|
| 앰버 (메인) | `amber-500` `#f59e0b` | 주요 버튼, 선택된 메뉴, 강조 |
| 민트 (포인트) | `mint-500` `#2fb585` | 로고, "확인된 정보"·안심 신호, 포인트 장식 |
| 크림 (배경) | `cream-50` `#fffbf5` | 화면 바탕 |
| 빨강 | `rose-500` | 삭제·오류, 나비 목걸이 |
| 갈색 | `#7c4a2d` | 일러스트 소품 |

- 민트는 **포인트에만** 쓴다. 화면의 주된 행동(버튼)은 앰버.
- 민트 위의 흰 글자, 흰 바탕 위의 작은 민트 글자는 명암이 부족하다. 작은 글자에는 `mint-700`, 민트 배경에는 진한 글자를 쓴다.
- 토큰은 `frontend/src/css/variables.css`의 `@theme`(Tailwind 클래스 `bg-mint-500`, `text-mint-700`, `bg-cream-50` 등).

## 5. 로고
- 민트색 지도 핀 안에 흰 발자국 + "Pet"(진한 갈회색) "Spot"(민트) 글자. 컴포넌트 `components/common/BrandLogo.vue`.
- 글꼴은 기존 글꼴을 유지한다(굵게).

## 6. 문구 톤
- 기본 안내 문구는 지금처럼 존댓말.
- 캐릭터 말풍선만 캐릭터 말투를 쓴다(콩이 반말, 나비 존댓말). 한 화면에 말풍선은 하나.

## 7. 캐릭터 등장 원칙
- **비었거나, 기다리거나, 칭찬할 때만** 등장한다.
- 정보를 읽는 화면(검색 결과 목록, 장소 정보 본문)에는 넣지 않는다.
- 장식용 그림은 화면 읽기 프로그램이 건너뛰게(`alt=""`, `aria-hidden`), 의미가 있는 그림만 설명을 붙인다.
- 움직임은 `prefers-reduced-motion` 사용자에게 끈다.

| 화면 | 캐릭터 | 장면 |
|---|---|---|
| 홈 첫 화면 | 둘 다 | 콩이가 앞장, 나비가 돋보기 들고 뒤따름 |
| 로딩 | 콩이 | 바닥 냄새 맡기 |
| 검색 결과 없음 | 콩이 | 고개 갸웃 |
| 사진 없는 장소 (숙박·공원·관광지 등) | 콩이 | 카메라 들기 |
| 사진 없는 장소 (카페·음식점) | 나비 | 돋보기로 살펴보기 |
| 동반 조건 안내 | 나비 | 말풍선 |
| 리뷰 작성 / 완료 | 나비 | 메모 작성 / 칭찬 |
| 즐겨찾기 없음 | 나비 | 빈 메모장 |
| 오류·404 | 콩이 | 지도를 거꾸로 들고 있음 |

## 8. 1차 그림 목록 (`TSK-DES-002`, 직접 제작)
| 파일 이름 | 장면 |
|---|---|
| `duo-together.png` | 둘이 함께 걷기 (콩이 손 흔들기, 나비 돋보기) |
| `kongi-sniff.png` | 콩이 바닥 냄새 맡기 |
| `kongi-curious.png` | 콩이 고개 갸웃 |
| `kongi-camera.png` | 콩이 카메라 들고 웃기 |
| `kongi-lost.png` | 콩이 지도를 거꾸로 들고 당황 |
| `nabi-inspect.png` | 나비 돋보기로 살펴보기 |
| `nabi-memo.png` | 나비 빈 메모장 들기 |
| `nabi-review.png` | 나비 메모장에 별 그리기 |
| `kongi-face.png`, `nabi-face.png` | 단순한 얼굴 아이콘 (정면, 털 질감 최소) |

### 생성 프롬프트
참고 이미지로 캐릭터 설정표(표정·동작 모음 시안)를 함께 넣는다. 공통 앞부분 + 장면을 이어 쓴다.

**공통 (영문 권장)**
```
Cute 3D character illustration, soft plush fur texture, rounded chubby proportions with a big head,
warm soft lighting, full body, centered, transparent background, no text, no letters, no logo.
```
**콩이 공통**
```
"Kongi": a young Pembroke Welsh Corgi dog. Amber-orange fur with a cream-white blaze from forehead
to chest and belly, NO stripes, big upright pointed ears with pink insides, black round dog nose,
happy open mouth with a pink tongue, short legs. Wears a mint-green bandana with a small white paw print
(no text) and a brown leather backpack.
```
**나비 공통**
```
"Nabi": an orange tabby cat. Light orange fur with darker orange stripes, cream muzzle and belly,
pink cat nose, calm half-closed eyes, red collar with a small gold bell. No backpack.
```
**장면**
- duo-together: `Kongi walking forward waving one paw, Nabi walking beside holding a magnifying glass, both smiling.`
- kongi-sniff: `Kongi with its nose to the ground, sniffing, eyes closed, tail up.`
- kongi-curious: `Kongi sitting, head tilted to one side, curious face, one ear slightly bent.`
- kongi-camera: `Kongi sitting and holding a small vintage camera with both paws, cheerful.`
- kongi-lost: `Kongi holding a paper map upside down, confused worried face, sweat drop.`
- nabi-inspect: `Nabi sitting and looking through a magnifying glass, one eye enlarged by the lens.`
- nabi-memo: `Nabi sitting and holding an empty small notepad toward the viewer, gentle smile.`
- nabi-review: `Nabi writing in a small notepad with a pencil, satisfied face, small stars floating.`
- *-face: `Simple flat front-facing head icon of (Kongi | Nabi), minimal fur detail, thick clean outline, reads clearly at 32px.`
