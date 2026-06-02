import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import banner1 from "@/assets/LCKbanner.png";
import banner2 from "@/assets/VALbanner.png";
import banner3 from "@/assets/PUBGbanner.png";
import banner4 from "@/assets/WATCHbanner.png";

const banners = [
  {image:banner1,
    link: "/events/3952",
   },
  {image:banner2,
    link: "/events/3954",
   },
  {image:banner3,
    link: "/events/3957",
   },
  {image:banner4,
    link: "/events/3955",
   },
  ];

export function HeroSection() {
  const [current, setCurrent] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrent((prev) => (prev + 1) % banners.length);
    }, 10000); // 10초

    return () => clearInterval(timer);
  }, []);

  return (
    <section className="flex justify-center px-6 py-6">
      <div className="relative mx-auto h-[400px] max-w-7x1 w-[1240px] overflow-hidden rounded-3xl shadow-xl ">
        <div
            className="flex h-full transition-transform duration-700 ease-in-out" 
            style={{
              transform: `translateX(-${current * 100}%)`,
              display: 'flex'
            }}
          >
          {banners.map((banner, index) => (
            <Link
              key={index}
              to={banner.link}
              className="relative h-full min-w-full block"
            >
          <div
            key={index}
            className="relative h-full min-w-full"
          >
            <img
              src={banner.image}
              alt={`banner-${index}`}
              className="h-full w-full object-cover scale-100"
            />

            <div className="absolute inset-0 bg-black/20 hover:bg-black/10 transition" />
            
          </div>
          </Link>
        ))}
      </div>


      {/* 하단 인디케이터 */}
      <div className="absolute bottom-6 left-1/2 z-20 flex -translate-x-1/2 gap-2">
        {banners.map((_, index) => (
          <button
            key={index}
            onClick={() => setCurrent(index)}
            className={`h-2 transition-all rounded-full ${
              current === index
                ? "w-8 bg-white"
                : "w-2 bg-white/50"
            }`}
          />
        ))}
      </div>
    </div>
    </section>
  );
}