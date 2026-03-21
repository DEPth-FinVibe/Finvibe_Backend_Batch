package depth.finvibe.modules.market.application.port.in;

import java.util.List;

import depth.finvibe.modules.market.dto.CategoryDto;
import depth.finvibe.modules.market.dto.CategoryInternalDto;

public interface CategoryQueryUseCase {
    List<CategoryInternalDto.Response> getAllCategoriesForInternal();

    CategoryDto.ChangeRateResponse getCategoryChangeRate(Long categoryId);
}
