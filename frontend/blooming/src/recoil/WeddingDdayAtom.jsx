import { atom } from "recoil";

// 웨딩 날짜 데이터
export const weddingDateState = atom({
  key: "weddingDateState",
  default: "2024-03-14",
});

export const weddingDdayState = atom({
  key: "weddingDdayState",
  default: "",
});