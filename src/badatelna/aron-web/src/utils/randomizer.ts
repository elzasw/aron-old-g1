import { OtherSource } from "../components";

export function getRandomInt(min: number, max: number, target: number = 0, strength: number = 0) {
  const random = Math.random();
  const range = max - min;
  const targetNormalized = (target - min) / range;

  const biased = random * (1 - strength) + targetNormalized * strength;

  return Math.floor(biased * (range + 1)) + min;
}

export function getRandomColor() {
  const h = getRandomInt(0, 360, 180, 0.1);
  const s = getRandomInt(20, 80, 50, 0.1);
  const l = getRandomInt(20, 50, 35, 0.1);
  return `hsl(${h}, ${s}%, ${l}%)`
}

export function getRandomIcon() {
  const icons = [
    "fas fa-book",
    "fas fa-building",
    "fas fa-map",
    "fas fa-house-user",
    "fas fa-address-card",
    "fas fa-user",
    "fas fa-users",
    "fas fa-user-edit",
    "fas fa-laptop-house",
    "fas fa-globe-americas",
    "fas fa-horse-head",
    "fas fa-fish",
    "fas fa-dog",
    "fas fa-frog",
    "fas fa-film",
    "fas fa-photo-video",
    "fas fa-sitemap",
    "fas fa-graduation-cap",
  ]
  return icons[getRandomInt(0, icons.length - 1)];
}

export function randomizeFavourites(favouriteQueries: OtherSource[]):OtherSource[] {
  return favouriteQueries.filter(() => getRandomInt(0, 1)).map((query) => {
    const hasLongText =  (query.label?.length || "") > 20 || (query.tooltip?.length || 0) > 40;
    const colSpan = hasLongText ? getRandomInt(1, 2) : 1;
    const rowSpan = hasLongText && colSpan === 1 ? 2 : 1;
    const max = 2;

    const randomColumn = colSpan < max ? getRandomInt(colSpan, 2, 1, 0.4) : colSpan;
    const randomRow = rowSpan < max ? getRandomInt(rowSpan, 2, 1, 0.4) : rowSpan;

    return {
      ...query,
      icon: { icon: getRandomIcon() },
      imageUrl: getRandomInt(0, 1) ? `https://picsum.photos/500/${getRandomInt(400, 500)}` : undefined,
      columnSpan: randomColumn,
      rowSpan: randomRow,
      backgroundColor: getRandomColor(),
    }
  })
}
